# Shuo Dian Sha — Sky Wuji Edition

An unofficial personal modification of [BryceWG/BiBi-Keyboard v4.4.5](https://github.com/BryceWG/BiBi-Keyboard/tree/v4.4.5), maintained by Sky无极君. The Android app label is **说点啥(Sky无极君特供版)**.

The original software is BiBi Keyboard (说点啥), by BryceWG. This is not an official upstream release. Original copyright, Apache 2.0 licensing and all third-party notices are retained.

[Download releases](https://github.com/skyXIANGTIAN13152/BiBi-Keyboard-Sky/releases/latest) · [Chinese documentation and build instructions](README.md) · [Upstream README](README_UPSTREAM_EN.md) · [Modifications](MODIFICATIONS.md) · [LICENSE](LICENSE) · [NOTICE](NOTICE)

Changes include a sculpted Cherry Ivory keyboard, English controls, a BACK label, the SKY.无极 watermark, FunASR Nano decoding chunks up to eight seconds, `/sil` cleanup, and suppression of exactly-zero PCM chunks. The original live audio waveform is retained. No energy threshold was added for quiet nonzero speech.

Version: **4.4.5-sky.1 (176)**. APK: ARM64, Android 8.0+. Package: `com.brycewg.asrkb.cherry`, retaining the earlier personal build identity and signing key. It installs separately from the upstream package. Updates are available manually from this repository; the upstream automatic updater is disabled for this distribution.

No private credentials, recordings, transcripts, signing keys or large ASR models are shipped in this repository. Models can be downloaded in the app; cloud services require the user's own setup. Build with JDK 21, Gradle wrapper 9.4.1, Android SDK 37 and Build Tools 36.0.0. See the Chinese README for commands and signing environment variables.
