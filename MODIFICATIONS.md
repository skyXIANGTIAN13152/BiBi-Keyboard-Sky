# 修改说明 / Modification notice

本项目是 `BryceWG/BiBi-Keyboard` 的非官方改版，基于 `v4.4.5`（提交 `12eb5c31ec72f8aff34c53eccbcca1f605e54a41`）。最近修改日期：2026-10-20；维护者：Sky无极君（skyXIANGTIAN13152）。

原始快照保留为仓库首个提交；原项目首页说明保留在 `README_UPSTREAM.md` 与 `README_UPSTREAM_EN.md`。

修改内容：樱桃红白立体键帽主题、英文控件、BACK 提示与 SKY.无极水印；独立应用 ID；FunASR Nano 最长 8 秒的识别分段；`/sil` 清理；全零音频保护；专供版名称及来源声明；本改版手动更新入口。

原版专用 GitHub Actions 流程移至 `upstream-reference/workflows/`，以 `.disabled` 结尾保留参考。原 LICENSE 逐字保留；NOTICE 保留原文并追加改版声明。原项目和第三方许可证仍适用。

## 2026-10-19 本地修复版 4.4.5-sky.2（177）

修复 FunASR Nano 长句识别成功后仍被界面提前超时丢弃的问题。手机历史确认 15～34 秒录音会在模型完成前被判定超时；同一请求随后正常产生文字。按使用者要求，取消 Nano 的自动处理超时，不创建等待结果的超时任务，直到识别成功、返回错误或手动取消；历史速度统计和备用模式的最终超时上限不能重新启用该计时器。保留原有 8 秒内部分段、专业词提示和静音标记清理。

IME 使用持续递增的会话编号，防止超时、取消或完成后的迟到回调覆盖下一次录音。其他仍使用有限超时的引擎达到等待上限时显示现有的识别超时提示。此版本仅作本地验证，修复现已合入公开版本 sky.4。

## 2026-10-20 本地优化版 4.4.5-sky.3（179）

FunASR Nano 的实时音频分段从通用的 15～24 秒改为模型专用的最长 8 秒片段。每次音频缓冲超过 8 秒时，优先在此前 6～8 秒之间已检测到的最长静音处切分；没有合适静音则切取 8 秒。以 200 毫秒音频帧输入时，首段可在约 8.2 秒开始处理。8 秒以内的短句保持整句，防止过早拆出短尾段。麦克风与外部 PCM 输入共用同一策略；已有的串行解码、尾段收口和最终单次交付保持不变。其他模型继续使用原有分段策略。保留 sky.2 的无自动处理超时、手动取消和会话隔离。构建 178 为本地性能候选，最终交付构建为 179。

## 2026-10-20 公开修复版 4.4.5-sky.4（180）

首次公开发布 sky.2 和 sky.3 的长句修复及识别等待优化。相对手机已验证的 sky.3（179），本次仅递增版本并整理公开发行文档和更新元数据，识别代码保持一致。沿用相同应用 ID 和签名，支持覆盖更新并保留用户数据。88 项回归测试和签名校验通过；公开验证记录只包含测试汇总，不附带个人录音、识别内容、账号或密钥。

## 修改或新增的应用源文件

- `.gitattributes`
- `.gitignore`
- `NOTICE`
- `README.md`
- `README_EN.md`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/assets/licenses/BiBi-Keyboard-LICENSE`
- `app/src/main/assets/licenses/BiBi-Keyboard-NOTICE`
- `app/src/main/java/com/brycewg/asrkb/asr/FunAsrNanoFileAsrEngine.kt`
- `app/src/main/java/com/brycewg/asrkb/asr/AsrTimeoutCalculator.kt`
- `app/src/main/java/com/brycewg/asrkb/asr/BaseFileAsrEngine.kt`
- `app/src/main/java/com/brycewg/asrkb/asr/GenericPushFileAsrAdapter.kt`
- `app/src/main/java/com/brycewg/asrkb/asr/NonStreamingProgressiveChunking.kt`
- `app/src/main/java/com/brycewg/asrkb/api/AsrRecognitionService.kt`
- `app/src/main/java/com/brycewg/asrkb/api/ExternalSpeechSession.kt`
- `app/src/main/java/com/brycewg/asrkb/asr/FunAsrNanoText.kt`
- `app/src/main/java/com/brycewg/asrkb/asr/LocalOfflinePcmChunking.kt`
- `app/src/main/java/com/brycewg/asrkb/ime/ImeKeyboardViewFactory.kt`
- `app/src/main/java/com/brycewg/asrkb/ime/AsrSessionManager.kt`
- `app/src/main/java/com/brycewg/asrkb/ime/AsrStoppedSessionGate.kt`
- `app/src/main/java/com/brycewg/asrkb/ime/KeyboardActionHandler.kt`
- `app/src/main/java/com/brycewg/asrkb/ime/ProcessingTimeoutController.kt`
- `app/src/main/java/com/brycewg/asrkb/ime/ImeLayoutController.kt`
- `app/src/main/java/com/brycewg/asrkb/ime/ImeUiRenderer.kt`
- `app/src/main/java/com/brycewg/asrkb/ime/layout/KeyboardLayoutRuntimeApplier.kt`
- `app/src/main/java/com/brycewg/asrkb/ime/layout/KeyboardLayoutStore.kt`
- `app/src/main/java/com/brycewg/asrkb/ui/BibiViewTheme.kt`
- `app/src/main/java/com/brycewg/asrkb/ui/floatingball/AsrSessionManager.kt`
- `app/src/main/java/com/brycewg/asrkb/ui/CherryKeyDrawable.kt`
- `app/src/main/java/com/brycewg/asrkb/ui/settings/compose/core/SettingsActionController.kt`
- `app/src/main/java/com/brycewg/asrkb/ui/settings/compose/screens/AboutSettingsScreen.kt`
- `app/src/main/java/com/brycewg/asrkb/ui/settings/compose/state/SettingsUpdateCoordinator.kt`
- `app/src/main/java/com/brycewg/asrkb/ui/widgets/CherryKeyViews.kt`
- `app/src/main/java/com/brycewg/asrkb/ui/widgets/PunctKeyView.kt`
- `app/src/main/res/values-ar/strings.xml`
- `app/src/main/res/values-ar/strings_about.xml`
- `app/src/main/res/values-ja/strings.xml`
- `app/src/main/res/values-ja/strings_about.xml`
- `app/src/main/res/values-zh-rCN/strings.xml`
- `app/src/main/res/values-zh-rCN/strings_about.xml`
- `app/src/main/res/values-zh-rTW/strings.xml`
- `app/src/main/res/values-zh-rTW/strings_about.xml`
- `app/src/main/res/values/bools.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/strings_about.xml`
- `app/src/main/res/values/strings_keyboard.xml`
- `app/src/main/res/values/strings_sky.xml`
- `app/src/main/res/xml/shortcuts.xml`
- `app/src/test/java/com/brycewg/asrkb/asr/FunAsrNanoAudioTest.kt`
- `app/src/test/java/com/brycewg/asrkb/asr/AsrTimeoutCalculatorTest.kt`
- `app/src/test/java/com/brycewg/asrkb/asr/NanoProgressiveChunkingTest.kt`
- `app/src/test/java/com/brycewg/asrkb/asr/GenericPushFileAsrAdapterTest.kt`
- `app/src/test/java/com/brycewg/asrkb/ime/AsrSessionManagerSessionIsolationTest.kt`
- `app/src/test/java/com/brycewg/asrkb/ime/ProcessingTimeoutControllerTest.kt`
- `app/src/test/java/com/brycewg/asrkb/api/ExternalSpeechSessionUnboundedWaitTest.kt`
- `app/src/test/java/com/brycewg/asrkb/asr/FunAsrNanoTextTest.kt`
- `app/src/test/java/com/brycewg/asrkb/asr/LocalOfflinePcmChunkingTest.kt`
- `version.json`
