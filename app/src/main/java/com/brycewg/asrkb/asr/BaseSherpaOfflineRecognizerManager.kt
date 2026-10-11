package com.brycewg.asrkb.asr

import android.content.res.AssetManager
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * sherpa-onnx 离线识别器 manager 公共骨架：
 * - 统一 recognizer 缓存、prepare 生命周期、回调保护与自动卸载；
 * - 具体模型仅负责类初始化、config 构建与 recognizer 创建。
 */
internal abstract class BaseSherpaOfflineRecognizerManager {

    protected abstract val tag: String

    protected val scope = CoroutineScope(SupervisorJob())
    protected val mutex = Mutex()

    @Volatile
    protected var cachedConfig: Any? = null

    @Volatile
    protected var cachedRecognizer: ReflectiveRecognizer? = null

    @Volatile
    protected var preparing: Boolean = false

    @Volatile
    private var unloadJob: Job? = null

    fun unload() {
        val snapshot = cachedRecognizer ?: return
        scope.launch {
            val shouldRelease = mutex.withLock {
                if (cachedRecognizer !== snapshot) return@withLock false
                cachedRecognizer = null
                cachedConfig = null
                unloadJob?.cancel()
                unloadJob = null
                true
            }
            if (shouldRelease) {
                try {
                    snapshot.release()
                } catch (t: Throwable) {
                    Log.e(tag, "Failed to release recognizer on unload", t)
                }
                Log.d(tag, "Recognizer unloaded")
            }
        }
    }

    fun isPrepared(): Boolean = cachedRecognizer != null

    fun isPreparing(): Boolean = preparing

    protected fun scheduleAutoUnload(keepAliveMs: Long, alwaysKeep: Boolean) {
        unloadJob = sherpaScheduleAutoUnload(tag, scope, unloadJob, keepAliveMs, alwaysKeep) {
            unload()
        }
    }

    protected fun releaseRecognizerSafely(recognizer: ReflectiveRecognizer?, reason: String) {
        if (recognizer == null) return
        try {
            recognizer.release()
        } catch (t: Throwable) {
            Log.e(tag, "Failed to release recognizer ($reason)", t)
        }
    }

    protected fun invokeCallbackSafely(name: String, callback: (() -> Unit)?) {
        sherpaInvokeCallbackSafely(tag, name, callback)
    }

    protected suspend fun ensurePreparedLocked(
        assetManager: AssetManager?,
        config: Any,
        onLoadStart: (() -> Unit)?,
        onLoadDone: (() -> Unit)?
    ): ReflectiveRecognizer? {
        initClasses()

        val cached = cachedRecognizer
        if (cached != null && cachedConfig == config) return cached

        preparing = true
        unloadJob?.cancel()
        unloadJob = null

        var newRecognizer: ReflectiveRecognizer? = null
        try {
            currentCoroutineContext().ensureActive()
            invokeCallbackSafely("onLoadStart", onLoadStart)
            currentCoroutineContext().ensureActive()

            val recognizerConfig = buildRecognizerConfig(config)
            currentCoroutineContext().ensureActive()
            val rawRecognizer = createRecognizer(assetManager, recognizerConfig)
            newRecognizer = ReflectiveRecognizer(rawRecognizer, offlineRecognizerClass())
            currentCoroutineContext().ensureActive()

            val oldRecognizer = cachedRecognizer
            cachedRecognizer = newRecognizer
            cachedConfig = config

            invokeCallbackSafely("onLoadDone", onLoadDone)
            if (oldRecognizer != null && oldRecognizer !== newRecognizer) {
                releaseRecognizerSafely(oldRecognizer, "old")
            }
            return newRecognizer
        } catch (t: CancellationException) {
            releaseRecognizerSafely(newRecognizer, "canceled")
            throw t
        } catch (t: Throwable) {
            releaseRecognizerSafely(newRecognizer, "failed")
            throw t
        } finally {
            preparing = false
        }
    }

    protected abstract fun initClasses()

    protected abstract fun buildRecognizerConfig(config: Any): Any

    protected abstract fun createRecognizer(assetManager: AssetManager?, recognizerConfig: Any): Any

    protected abstract fun offlineRecognizerClass(): Class<*>
}
