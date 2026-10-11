// fxliang fcitx5 语音输入 Provider 协议描述；包名和接口名需保持与对端一致。
package org.fcitx.fcitx5.android.common.ipc;

import android.os.Bundle;
import org.fcitx.fcitx5.android.common.ipc.IVoiceInputCallback;

interface IVoiceInputProvider {
    boolean isAvailable();

    Bundle getPreferredConfig();

    oneway void configure(in Bundle config);

    oneway void startSession(IVoiceInputCallback callback);

    oneway void feedAudio(in byte[] pcm, int offset, int len, long ptsMs);

    oneway void endStream();

    oneway void cancelSession();

    oneway void stopSession();
}
