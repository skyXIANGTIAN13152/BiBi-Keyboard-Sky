package com.brycewg.asrkb.ui.floatingball

import com.brycewg.asrkb.BuildConfig

/**
 * 悬浮球状态机
 * 使用 sealed class 定义所有可能的状态，替代多个布尔标志
 */
sealed class FloatingBallState {
    /** 空闲状态 */
    object Idle : FloatingBallState()

    /** 录音中 */
    object Recording : FloatingBallState()

    /** 处理中（上传/识别/AI后处理） */
    object Processing : FloatingBallState()

    /** 错误状态（带错误消息） */
    data class Error(val message: String) : FloatingBallState()

    /** 移动模式（用户可拖动悬浮球） */
    object MoveMode : FloatingBallState()
}

/**
 * 状态转换辅助类
 * 管理状态机的合法转换，确保状态一致性
 */
class FloatingBallStateMachine {
    private var currentState: FloatingBallState = FloatingBallState.Idle

    val state: FloatingBallState
        get() = currentState

    fun transitionTo(newState: FloatingBallState) {
        if (currentState == newState) return
        // 记录状态转换（便于调试）
        if (BuildConfig.DEBUG) {
            android.util.Log.d(
                "FloatingBallStateMachine",
                "State transition: $currentState -> $newState"
            )
        }
        currentState = newState
    }

    /** 是否正在录音 */
    val isRecording: Boolean
        get() = currentState is FloatingBallState.Recording

    /** 是否正在处理 */
    val isProcessing: Boolean
        get() = currentState is FloatingBallState.Processing

    /** 是否处于错误状态 */
    val isError: Boolean
        get() = currentState is FloatingBallState.Error

    /** 是否处于移动模式 */
    val isMoveMode: Boolean
        get() = currentState is FloatingBallState.MoveMode

    /** 是否空闲 */
    val isIdle: Boolean
        get() = currentState is FloatingBallState.Idle
}
