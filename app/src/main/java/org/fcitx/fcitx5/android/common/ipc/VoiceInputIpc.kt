/**
 * fxliang fcitx5 语音输入 Provider 协议常量。
 *
 * 归属模块：api
 */
package org.fcitx.fcitx5.android.common.ipc

object VoiceInputIpc {
    const val SERVICE_ACTION_SUFFIX = ".plugin.VOICE_INPUT"
    const val COMMIT_ACTION_SUFFIX = ".plugin.VOICE_INPUT_COMMIT"
    const val PARTIAL_ACTION_SUFFIX = ".plugin.VOICE_INPUT_PARTIAL"
    const val START_FLOATING_ACTION_SUFFIX = ".plugin.VOICE_INPUT_FLOATING"
    const val EXTRA_COMMIT_TEXT = "commitText"
    const val EXTRA_PARTIAL_TEXT = "partialText"

    object ConfigKeys {
        const val SAMPLE_RATE = "sampleRate"
        const val BITS_PER_SAMPLE = "bitsPerSample"
        const val CHANNELS = "channels"
        const val SILENCE_MS = "silenceMs"
        const val LANGUAGE = "language"
    }

    object ErrorCodes {
        const val UNKNOWN = 0
        const val NOT_AVAILABLE = 1
        const val MODEL_LOAD_FAILED = 2
        const val DECODE_FAILED = 3
        const val INVALID_AUDIO = 4
    }
}
