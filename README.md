# 说点啥(Sky无极君特供版)

基于 **[BryceWG/BiBi-Keyboard「说点啥」v4.4.5](https://github.com/BryceWG/BiBi-Keyboard/tree/v4.4.5)** 的非官方个人改版，由 **Sky无极君** 维护。

原项目及原作者是 **BryceWG / BiBi Keyboard**。本项目保留原作者版权与 Apache 2.0 许可证，不代表原作者发布或认可此改版。基础输入法、语音识别能力及开源依赖来自原项目；本改版主要调整外观、品牌显示和部分本地识别处理。

**[下载专供版安装包](https://github.com/skyXIANGTIAN13152/BiBi-Keyboard-Sky/releases/latest)** · [修改说明](MODIFICATIONS.md) · [原项目说明](README_UPSTREAM.md) · [LICENSE](LICENSE) · [NOTICE](NOTICE)

## 这一版做了什么

- 樱桃红与象牙白配色，凸起顶面、斜切侧壁、高光和底部阴影，形成机械键帽质感。
- 英文操作提示，返回打字键盘的按键增加 **BACK**，SPACE 右下方保留小水印 **SKY.无极**。
- 保留随声音振幅变化的波形。
- FunASR Nano 在内部按最长 8 秒分段识别并顺序合并，优先在停顿处切分，改善长句被截断的问题。
- 清理混入正文或句尾的 `/sil` 静音标记；跳过采样值全为零的音频片段，避免模型对完全无信号片段生成多余文字。没有新增音量阈值，微弱的非零声音仍保留。
- 应用名称改为 **说点啥(Sky无极君特供版)**；“关于”页标明原项目来源，并提供专供版下载入口。

## 安装与使用

当前版本：**4.4.5-sky.1（176）**。当前 APK 面向 **ARM64 Android 8.0 及以上**设备。

1. 在 [Releases](https://github.com/skyXIANGTIAN13152/BiBi-Keyboard-Sky/releases) 下载 APK 后安装。
2. 新用户按应用引导启用语音输入法并授予麦克风权限。
3. 使用本地识别时，在应用中下载对应模型；APK 不附带大型语音识别模型。使用云端服务时，自行配置服务与 API 凭证。

应用 ID 为 `com.brycewg.asrkb.cherry`，沿用此前 BiBi Cherry 个人版的应用 ID 与签名，可以覆盖更新该个人版并保留数据。它与原版 `com.brycewg.asrkb` 独立安装。

本改版通过本仓库的 **Releases** 手动更新，已关闭原版自动更新源，避免安装到其他发行版。应用“关于”页的 **Download Sky Edition Updates** 可打开下载入口。

本仓库与发行包不包含维护者的账号配置、API 密钥、录音、识别历史或签名私钥。这里提供的是开源改版程序，不提供原作者的云服务额度或付费授权。

## 来源与验证

上游版本：`v4.4.5`；上游提交：[`12eb5c31ec72f8aff34c53eccbcca1f605e54a41`](https://github.com/BryceWG/BiBi-Keyboard/commit/12eb5c31ec72f8aff34c53eccbcca1f605e54a41)。上游归档校验记录见 [UPSTREAM.json](UPSTREAM.json)。

25 项静音标记、全零音频、分段及合并测试已通过。识别修复在 HONOR Android 15 手机上用约 51 秒的长停顿样本复测，输出与未加停顿的原语音一致。改名版本保留同一识别修复。测试结果不代表所有设备或噪声环境下的识别准确率保证。

## 从源码构建

使用 JDK 21、Android SDK 37 / Build Tools 36.0.0 和仓库中的 Gradle 9.4.1 wrapper。将 `ANDROID_HOME` 指向本机 SDK，或创建不纳入版本控制的 `local.properties`。

```sh
./gradlew :app:testDebugUnitTest --tests com.brycewg.asrkb.asr.FunAsrNanoTextTest --tests com.brycewg.asrkb.asr.FunAsrNanoAudioTest --tests com.brycewg.asrkb.asr.LocalOfflinePcmChunkingTest --tests com.brycewg.asrkb.asr.NonStreamingProgressiveChunkingTest -PabiFilters=arm64-v8a
./gradlew :app:assembleDebug -PabiFilters=arm64-v8a
```

Windows 使用 `gradlew.bat`。发布构建使用 `:app:assembleRelease`，通过 `KEYSTORE_FILE`、`KEYSTORE_PASSWORD`、`KEY_ALIAS` 和 `KEY_PASSWORD` 环境变量指定自己的签名。不要提交签名文件或密码。官方提供的本改版发布包使用维护者持续保管的签名，其他人自行签名的 APK 不能直接覆盖它。

默认不设置 `SF_FREE_API_KEY` 与 `POCKETBASE_BASE_URL`。仓库包含构建所需的上游 sherpa-onnx AAR；大型 ASR 模型由应用按需下载。

## 许可证与致谢

保留 **Copyright 2025 BryceWG** 和原项目的 **[Apache License 2.0](LICENSE)**。原 NOTICE 逐字保留，并追加本改版声明；修改过的应用源文件带有修改标记，详细列表见 [MODIFICATIONS.md](MODIFICATIONS.md)。

sherpa-onnx、TEN VAD、Miuix、Phosphor Icons、WaveLineView 等组件的各自许可和附加条件见 [NOTICE](NOTICE) 及 `app/src/main/assets/licenses/`。本仓库不改变这些第三方许可。完整许可证也可在应用“关于”页查看。
