// Modified for Sky Wuji Edition on 2026-10-19; based on BryceWG/BiBi-Keyboard v4.4.5. See MODIFICATIONS.md.
/**
 * 按 IME 录音会话去重 onStopped 终态交付。
 *
 * 归属模块：ime
 */
package com.brycewg.asrkb.ime

internal class AsrStoppedSessionGate {
    private val lock = Any()
    private var deliveredSessionSeq = 0L

    /**
     * 新录音开始时清除上一轮的交付记录；迟到回调由会话管理器的唯一序号校验拦截。
     */
    fun reset() {
        synchronized(lock) { deliveredSessionSeq = 0L }
    }

    fun tryDeliver(sessionSeq: Long): Boolean {
        if (sessionSeq == 0L) return false
        return synchronized(lock) {
            if (deliveredSessionSeq == sessionSeq) {
                false
            } else {
                deliveredSessionSeq = sessionSeq
                true
            }
        }
    }
}
