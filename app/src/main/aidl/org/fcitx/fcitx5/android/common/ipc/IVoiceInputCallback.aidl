// fxliang fcitx5 语音输入回调协议描述；事务顺序需保持与对端一致。
package org.fcitx.fcitx5.android.common.ipc;

oneway interface IVoiceInputCallback {
    void onReady();

    void onVolumeLevel(int rms);

    void onPartialResult(String text);

    void onSegmentFinal(String text);

    void onSessionEnded();

    void onError(int code, String message);
}
