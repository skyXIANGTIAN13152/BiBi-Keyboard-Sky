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

当前版本：**4.4.5-sky.4（180）**。FunASR Nano 在长句录音期间提前分段识别，减少停录后的等待；8 秒以内的短句保持整句处理。取消 Nano 的自动处理超时，仍可手动取消，并隔离上一次会话的迟到回调。详见 [修改说明](MODIFICATIONS.md)。当前 APK 面向 **ARM64 Android 8.0 及以上**设备。

1. 在 [Releases](https://github.com/skyXIANGTIAN13152/BiBi-Keyboard-Sky/releases) 下载 APK 后安装。
2. 新用户按应用引导启用语音输入法并授予麦克风权限。
3. 使用本地识别时，在应用中下载对应模型；APK 不附带大型语音识别模型。使用云端服务时，自行配置服务与 API 凭证。

GitHub 会自动简化包含中文的附件文件名，因此提供两种下载：

- **直接安装 APK**：`SaySomething-Sky-Wuji-4.4.5-sky.4-arm64.apk`，安装后应用名称为 **说点啥(Sky无极君特供版)**。
- **保留中文安装包名称的 ZIP**：`SaySomething-Sky-Wuji-4.4.5-sky.4-arm64.zip`，解压后得到 **说点啥(Sky无极君特供版).apk**。

两种下载包含同一个已签名安装包，任选一种即可。

应用 ID 为 `com.brycewg.asrkb.cherry`，沿用此前 BiBi Cherry 个人版的应用 ID 与签名，可以覆盖更新该个人版并保留数据。它与原版 `com.brycewg.asrkb` 独立安装。

本改版通过本仓库的 **Releases** 手动更新，已关闭原版自动更新源，避免安装到其他发行版。应用“关于”页的 **Download Sky Edition Updates** 可打开下载入口。

本仓库与发行包不包含维护者的账号配置、API 密钥、录音、识别历史或签名私钥。这里提供的是开源改版程序，不提供原作者的云服务额度或付费授权。

## 来源与验证

上游版本：`v4.4.5`；上游提交：[`12eb5c31ec72f8aff34c53eccbcca1f605e54a41`](https://github.com/BryceWG/BiBi-Keyboard/commit/12eb5c31ec72f8aff34c53eccbcca1f605e54a41)。上游归档校验记录见 [UPSTREAM.json](UPSTREAM.json)。

88 项回归测试通过，覆盖短句保护、长音频完整性、分段合并、手动取消、无超时等待和迟到结果隔离。相同识别代码的 sky.3 构建已在 Android 15 手机上回放 6 条短句、长句及长难句样本（3.8～33.6 秒），均正常返回一次最终结果；四条长句的平均停录后等待从 25.31 秒降至 20.20 秒。短句未确认稳定提速，个别长句的字词和标点有变化；这是单设备样本比较，不是准确率保证。sky.4 仅更新发行版本与公开文档，识别代码与该已测构建一致。详情见 [验证记录](RELEASE_VALIDATION.json)。

## 从源码构建

使用 JDK 21、Android SDK 37 / Build Tools 36.0.0 和仓库中的 Gradle 9.4.1 wrapper。将 `ANDROID_HOME` 指向本机 SDK，或创建不纳入版本控制的 `local.properties`。

```sh
./gradlew :app:testDebugUnitTest --tests com.brycewg.asrkb.asr.FunAsrNanoTextTest --tests com.brycewg.asrkb.asr.FunAsrNanoAudioTest --tests com.brycewg.asrkb.asr.LocalOfflinePcmChunkingTest --tests com.brycewg.asrkb.asr.NonStreamingProgressiveChunkingTest -PabiFilters=arm64-v8a
./gradlew :app:assembleDebug -PabiFilters=arm64-v8a
```

Windows 使用 `gradlew.bat`。发布构建使用 `:app:assembleRelease`，通过 `KEYSTORE_FILE`、`KEYSTORE_PASSWORD`、`KEY_ALIAS` 和 `KEY_PASSWORD` 环境变量指定自己的签名。不要提交签名文件或密码。本仓库提供的发布包使用维护者持续保管的签名，其他人自行签名的 APK 不能直接覆盖它。

默认不设置 `SF_FREE_API_KEY` 与 `POCKETBASE_BASE_URL`。仓库包含构建所需的上游 sherpa-onnx AAR；大型 ASR 模型由应用按需下载。

## 许可证与致谢

保留 **Copyright 2025 BryceWG** 和原项目的 **[Apache License 2.0](LICENSE)**。原 NOTICE 逐字保留，并追加本改版声明；修改过的应用源文件带有修改标记，详细列表见 [MODIFICATIONS.md](MODIFICATIONS.md)。

sherpa-onnx、TEN VAD、Miuix、Phosphor Icons、WaveLineView 等组件的各自许可和附加条件见 [NOTICE](NOTICE) 及 `app/src/main/assets/licenses/`。本仓库不改变这些第三方许可。完整许可证也可在应用“关于”页查看。
