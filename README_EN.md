# Shuo Dian Sha — Sky Wuji Edition

An unofficial personal modification of [BryceWG/BiBi-Keyboard v4.4.5](https://github.com/BryceWG/BiBi-Keyboard/tree/v4.4.5), maintained by Sky无极君. The Android app label is **说点啥(Sky无极君特供版)**.

The original software is BiBi Keyboard (说点啥), by BryceWG. This is not an official upstream release. Original copyright, Apache 2.0 licensing and all third-party notices are retained.

[Download releases](https://github.com/skyXIANGTIAN13152/BiBi-Keyboard-Sky/releases/latest) · [Chinese documentation and build instructions](README.md) · [Upstream README](README_UPSTREAM_EN.md) · [Modifications](MODIFICATIONS.md) · [LICENSE](LICENSE) · [NOTICE](NOTICE)

Changes include a sculpted Cherry Ivory keyboard, English controls, a BACK label, the SKY.无极 watermark, FunASR Nano decoding chunks up to eight seconds, `/sil` cleanup, and suppression of exactly-zero PCM chunks. The original live audio waveform is retained. No energy threshold was added for quiet nonzero speech.

Published version: **4.4.5-sky.4 (180)**. Nano recognition starts during long recordings to reduce the wait after speech ends, while keeping utterances of up to 8 seconds whole. Automatic processing timeout is disabled for Nano; manual cancellation and isolation of late callbacks are retained. See [Modifications](MODIFICATIONS.md). APK: ARM64, Android 8.0+. Package: `com.brycewg.asrkb.cherry`, retaining the earlier personal build identity and signing key. It installs separately from the upstream package. Updates are available manually from this repository; the upstream automatic updater is disabled for this distribution.

GitHub normalizes non-ASCII attachment filenames. Install `SaySomething-Sky-Wuji-4.4.5-sky.4-arm64.apk` directly, or download `SaySomething-Sky-Wuji-4.4.5-sky.4-arm64.zip` to extract the exact filename **说点啥(Sky无极君特供版).apk**. Both contain the same signed APK and display the full Chinese app name after installation.

The release passes 88 regression tests. The same recognition code in sky.3 was tested on Android 15 with six short, long and complex utterances (3.8–33.6 seconds), each returning one final result. Mean post-stop waiting time for four long samples fell from 25.31 to 20.20 seconds. Short-speech speedup was not established and some long outputs changed words or punctuation. This is a single-device sample comparison, not an accuracy guarantee. sky.4 updates release metadata and documentation only; recognition code is unchanged from the tested build. See [validation](RELEASE_VALIDATION.json).

No private credentials, recordings, transcripts, signing keys or large ASR models are shipped in this repository. Models can be downloaded in the app; cloud services require the user's own setup. Build with JDK 21, Gradle wrapper 9.4.1, Android SDK 37 and Build Tools 36.0.0. See the Chinese README for commands and signing environment variables.
