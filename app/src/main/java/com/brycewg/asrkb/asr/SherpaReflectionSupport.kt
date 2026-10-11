package com.brycewg.asrkb.asr

import android.content.res.AssetManager
import android.util.Log
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * sherpa-onnx 反射适配公共辅助：
 * - 收敛离线识别器 wrapper、字段写入、回调保护与自动卸载调度；
 * - 供多个本地模型 manager 复用，减少重复兼容代码。
 */

internal class ReflectiveStream(val instance: Any) {
    val streamClass: Class<*> = instance.javaClass
    private val acceptWaveformWithSampleRateMethod: Method? = try {
        streamClass.getMethod(
            "acceptWaveform",
            FloatArray::class.java,
            Int::class.javaPrimitiveType
        )
    } catch (_: Throwable) {
        null
    }
    private val acceptWaveformMethod: Method? = try {
        streamClass.getMethod("acceptWaveform", FloatArray::class.java)
    } catch (_: Throwable) {
        null
    }
    private val releaseMethod: Method? = try {
        streamClass.getMethod("release")
    } catch (_: Throwable) {
        null
    }

    fun acceptWaveform(samples: FloatArray, sampleRate: Int) {
        val withSampleRate = acceptWaveformWithSampleRateMethod
        if (withSampleRate != null) {
            try {
                withSampleRate.invoke(instance, samples, sampleRate)
                return
            } catch (t: Throwable) {
                Log.d("ReflectiveStream", "Failed with sampleRate param, trying without", t)
            }
        }
        try {
            val withoutSampleRate = acceptWaveformMethod
                ?: throw NoSuchMethodException("acceptWaveform")
            withoutSampleRate.invoke(instance, samples)
        } catch (t: Throwable) {
            Log.e("ReflectiveStream", "acceptWaveform failed", t)
            throw IllegalStateException("acceptWaveform failed", t)
        }
    }

    fun release() {
        try {
            (releaseMethod ?: throw NoSuchMethodException("release")).invoke(instance)
        } catch (t: Throwable) {
            Log.e("ReflectiveStream", "Failed to release stream", t)
        }
    }
}

/**
 * sherpa-onnx 离线识别器的反射包装。
 */
internal class ReflectiveRecognizer(
    private val instance: Any,
    private val clsOfflineRecognizer: Class<*>
) {
    private val createStreamMethod: Method = clsOfflineRecognizer.getMethod("createStream")
    private val releaseMethod: Method? = try {
        clsOfflineRecognizer.getMethod("release")
    } catch (_: Throwable) {
        null
    }
    private val decodeMethodCache = ConcurrentHashMap<Class<*>, Method>()
    private val getResultMethodCache = ConcurrentHashMap<Class<*>, Method>()
    private val textFieldCache = ConcurrentHashMap<Class<*>, Field>()

    fun createStream(): ReflectiveStream {
        val stream = createStreamMethod.invoke(instance)
            ?: throw IllegalStateException("Failed to create stream")
        return ReflectiveStream(stream)
    }

    fun decode(stream: ReflectiveStream): String? {
        val clsStream = stream.streamClass
        val decodeMethod = decodeMethodCache.getOrPut(clsStream) {
            clsOfflineRecognizer.getMethod("decode", clsStream)
        }
        val getResultMethod = getResultMethodCache.getOrPut(clsStream) {
            clsOfflineRecognizer.getMethod("getResult", clsStream)
        }
        decodeMethod.invoke(instance, stream.instance)
        val result = getResultMethod.invoke(instance, stream.instance)
        return tryGetStringField(result, "text") ?: result?.toString()
    }

    fun release() {
        try {
            (releaseMethod ?: throw NoSuchMethodException("release")).invoke(instance)
        } catch (t: Throwable) {
            Log.e("ReflectiveRecognizer", "Failed to release recognizer", t)
        }
    }

    private fun tryGetStringField(target: Any?, name: String): String? {
        if (target == null) return null
        val field = textFieldCache[target.javaClass] ?: try {
            target.javaClass.getDeclaredField(name).apply {
                isAccessible = true
            }.also {
                textFieldCache[target.javaClass] = it
            }
        } catch (t: Throwable) {
            Log.d("ReflectiveRecognizer", "Failed to get string field '$name'", t)
            return null
        }
        return try {
            field.get(target) as? String
        } catch (t: Throwable) {
            Log.d("ReflectiveRecognizer", "Failed to read string field '$name'", t)
            null
        }
    }
}

internal fun sherpaIsClassAvailable(tag: String, vararg classNames: String): Boolean = try {
    classNames.forEach { Class.forName(it) }
    true
} catch (t: Throwable) {
    Log.d(tag, "sherpa-onnx not available", t)
    false
}

internal fun sherpaInvokeCallbackSafely(
    tag: String,
    name: String,
    callback: (() -> Unit)?
) {
    if (callback == null) return
    try {
        callback()
    } catch (t: Throwable) {
        Log.e(tag, "$name callback failed", t)
    }
}

internal fun sherpaTrySetField(tag: String, target: Any, name: String, value: Any?): Boolean = try {
    val field = target.javaClass.getDeclaredField(name)
    field.isAccessible = true
    field.set(target, value)
    true
} catch (t: Throwable) {
    try {
        val methodName = "set" + name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
        val setter = target.javaClass.methods.firstOrNull { method ->
            method.name == methodName &&
                method.parameterCount == 1 &&
                (value == null || isSherpaSetterCompatible(method.parameterTypes[0], value.javaClass))
        } ?: throw NoSuchMethodException("No compatible setter for $methodName")
        setter.invoke(target, value)
        true
    } catch (t2: Throwable) {
        Log.w(tag, "Failed to set field '$name'", t2)
        false
    }
}

internal fun sherpaCreateOfflineRecognizer(
    tag: String,
    recognizerClass: Class<*>,
    recognizerConfigClass: Class<*>,
    assetManager: AssetManager?,
    recognizerConfig: Any
): Any {
    val ctor = if (assetManager == null) {
        try {
            recognizerClass.getDeclaredConstructor(recognizerConfigClass)
        } catch (t: Throwable) {
            Log.d(tag, "No single-param constructor, using AssetManager variant", t)
            recognizerClass.getDeclaredConstructor(AssetManager::class.java, recognizerConfigClass)
        }
    } else {
        try {
            recognizerClass.getDeclaredConstructor(AssetManager::class.java, recognizerConfigClass)
        } catch (t: Throwable) {
            Log.d(tag, "No AssetManager constructor, using single-param variant", t)
            recognizerClass.getDeclaredConstructor(recognizerConfigClass)
        }
    }

    return if (ctor.parameterCount == 2) {
        ctor.newInstance(assetManager, recognizerConfig)
    } else {
        ctor.newInstance(recognizerConfig)
    }
}

internal fun sherpaScheduleAutoUnload(
    tag: String,
    scope: CoroutineScope,
    currentJob: Job?,
    keepAliveMs: Long,
    alwaysKeep: Boolean,
    unload: () -> Unit
): Job? {
    currentJob?.cancel()

    if (alwaysKeep) {
        Log.d(tag, "Recognizer will be kept alive indefinitely")
        return null
    }

    if (keepAliveMs <= 0L) {
        Log.d(tag, "Auto-unloading immediately (keepAliveMs=$keepAliveMs)")
        unload()
        return null
    }

    Log.d(tag, "Scheduling auto-unload in ${keepAliveMs}ms")
    return scope.launch {
        delay(keepAliveMs)
        Log.d(tag, "Auto-unloading recognizer after timeout")
        unload()
    }
}

private fun isSherpaSetterCompatible(parameterType: Class<*>, valueClass: Class<*>): Boolean {
    val boxedParameterType = boxSherpaType(parameterType)
    val boxedValueType = boxSherpaType(valueClass)
    return boxedParameterType.isAssignableFrom(boxedValueType)
}

private fun boxSherpaType(type: Class<*>): Class<*> = when (type) {
    Boolean::class.javaPrimitiveType -> Boolean::class.java
    Byte::class.javaPrimitiveType -> Byte::class.java
    Char::class.javaPrimitiveType -> Char::class.java
    Double::class.javaPrimitiveType -> Double::class.java
    Float::class.javaPrimitiveType -> Float::class.java
    Int::class.javaPrimitiveType -> Int::class.java
    Long::class.javaPrimitiveType -> Long::class.java
    Short::class.javaPrimitiveType -> Short::class.java
    java.lang.Void.TYPE -> java.lang.Void::class.java
    else -> type
}
