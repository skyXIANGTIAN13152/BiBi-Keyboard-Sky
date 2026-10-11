package com.brycewg.asrkb.aidl;

import com.brycewg.asrkb.aidl.SpeechConfig;
import com.brycewg.asrkb.aidl.ISpeechCallback;

interface IExternalSpeechService {
  /**
   * 启动一次语音识别会话。
   * @return 正整数 sessionId 表示成功；负数为错误码（-1: not_pro, -2: busy, -3: invalid, -4: no_permission）。
   */
  int startSession(in SpeechConfig config, ISpeechCallback callback);

  /** 主动停止录音，进入处理阶段（如有）。*/
  void stopSession(int sessionId);

  /** 取消并清理会话（不保证产生最终结果）。*/
  void cancelSession(int sessionId);

  /** 查询指定会话是否处于录音中。*/
  boolean isRecording(int sessionId);

  /** 是否存在任意活动会话。*/
  boolean isAnyRecording();

  /** 版本信息（语义化版本名）。*/
  String getVersion();

  // ================= 推送 PCM 模式（小企鹅侧录音）=================
  /**
   * 启动一次“上行PCM”会话（客户端录音并持续 writePcm 推送）。
   * 仍返回正整数 sessionId 表示成功；负数错误码（-2: busy, -3: invalid, -5: unsupported）。
   */
  int startPcmSession(in SpeechConfig config, ISpeechCallback callback);

  /**
   * 推送一帧 PCM16LE 单声道音频（建议 200ms 一包）。
   * 采样率固定 16000；channels 固定 1；不匹配时服务端可忽略该帧。
   */
  void writePcm(int sessionId, in byte[] pcm, int sampleRate, int channels);

  /**
   * 结束 PCM 推送并进入处理阶段，等价于 stopSession。
   */
  void finishPcm(int sessionId);
}
