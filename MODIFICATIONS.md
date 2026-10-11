# 修改说明 / Modification notice

本项目是 `BryceWG/BiBi-Keyboard` 的非官方改版，基于 `v4.4.5`（提交 `12eb5c31ec72f8aff34c53eccbcca1f605e54a41`）。修改日期：2026-10-11；维护者：Sky无极君（skyXIANGTIAN13152）。

原始快照保留为仓库首个提交；原项目首页说明保留在 `README_UPSTREAM.md` 与 `README_UPSTREAM_EN.md`。

修改内容：樱桃红白立体键帽主题、英文控件、BACK 提示与 SKY.无极水印；独立应用 ID；FunASR Nano 最长 8 秒的识别分段；`/sil` 清理；全零音频保护；专供版名称及来源声明；本改版手动更新入口。

原版专用 GitHub Actions 流程移至 `upstream-reference/workflows/`，以 `.disabled` 结尾保留参考。原 LICENSE 逐字保留；NOTICE 保留原文并追加改版声明。原项目和第三方许可证仍适用。

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
- `app/src/main/java/com/brycewg/asrkb/asr/FunAsrNanoText.kt`
- `app/src/main/java/com/brycewg/asrkb/asr/LocalOfflinePcmChunking.kt`
- `app/src/main/java/com/brycewg/asrkb/ime/ImeKeyboardViewFactory.kt`
- `app/src/main/java/com/brycewg/asrkb/ime/ImeLayoutController.kt`
- `app/src/main/java/com/brycewg/asrkb/ime/ImeUiRenderer.kt`
- `app/src/main/java/com/brycewg/asrkb/ime/layout/KeyboardLayoutRuntimeApplier.kt`
- `app/src/main/java/com/brycewg/asrkb/ime/layout/KeyboardLayoutStore.kt`
- `app/src/main/java/com/brycewg/asrkb/ui/BibiViewTheme.kt`
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
- `app/src/test/java/com/brycewg/asrkb/asr/FunAsrNanoTextTest.kt`
- `app/src/test/java/com/brycewg/asrkb/asr/LocalOfflinePcmChunkingTest.kt`
- `version.json`
