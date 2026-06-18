# FieldMark

> Field photo annotation with embedded compass heading, professional engineering measurements, and print-ready PDF export — built for engineers, electricians, and site supervisors.

**Mizanur Rahman** is a free, open-source Android app that turns your phone into a smart field-documentation tool. Capture a photo, mark it up with professional arrows, engineering-dimension measurements, callout text notes, and freehand handwriting — and the current compass heading, pitch, roll, and timestamp are **automatically stamped onto the image**. Export a print-ready engineering PDF — all offline, in **English or বাংলা**.

---

## ✨ Features

| Feature | Description |
| --- | --- |
| 📷 **Camera capture with live compass** | In-app high-resolution camera (CameraX) with a live compass-rose overlay while you frame the shot. |
| 🧭 **Auto-embedded compass stamp** | When you press the shutter, the current **heading (N/NE/E/SE/…°), pitch, roll, and timestamp** are captured and rendered as a professional metadata stamp on the photo — looks like an engineering EXIF overlay. |
| ✏️ **Professional vector annotation** | Filled-triangle arrows, engineering-dimension measurements (extension lines + dimension line with break + double arrows), callout text notes (numbered marker + leader line + text box), freehand, line, rectangle, circle — with color and stroke-width controls. |
| 📏 **Calibrated measurements** | Drag-to-measure with configurable pixels-per-unit (mm, cm, m, in, ft…). |
| 🕶️ **2D → 3D Anaglyph** | Convert any photo into a red-cyan stereoscopic 3D image (view with cheap 3D glasses). |
| 📄 **Print-ready PDF** | Export the annotated image (including the compass stamp) with a full engineering title block (project, location, inspector, date, report number, notes) — share via WhatsApp, email, or Bluetooth thermal printer. |
| ↩️ **Undo / Redo** | Multi-step undo/redo for every annotation. |
| 🌗 **Material 3 + dark mode** | Polished industrial UI with full dark mode and dynamic color on Android 12+. |
| 🌍 **Bilingual** | English (default) and বাংলা (Bangla) — switchable in-app, per-app locale via `AppCompatDelegate`. |
| 🔒 **Offline-first** | All core features work without internet. No accounts, no tracking. |

---

## 📥 Download the APK

> The latest signed debug APK is **automatically built by GitHub Actions** on every push and published as a workflow artifact.
> Tagged releases (`v*`) are also published to **GitHub Releases**.

1. Go to the [Actions](../../actions) tab → pick the latest green build → scroll to **Artifacts** → download `FieldMark-APK`.
2. Transfer to your Android device and install (you may need to enable *Install unknown apps* for your file manager / browser).
3. The package is `com.fieldmark.app.debug` for debug builds and `com.fieldmark.app` for release builds. The app displays as **Mizanur Rahman**.

> Want a **signed release**? Add these repository secrets and the workflow will sign the release APK automatically:
> - `DEBUG_KEYSTORE_B64` — base64 of your `.jks` file
> - `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

---

## 🛠 Build it yourself

The whole project builds in the cloud — **no Android Studio, no local JDK, no SDK download** required.

```bash
git clone https://github.com/seyamsakibulhasan/FieldMark.git
cd FieldMark
# Push to your own repo and let GitHub Actions build it.
```

### Local build (optional)

Requires JDK 17 and Android SDK 34.

```bash
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:assembleRelease        # release APK (signing in app/build.gradle.kts)
```

---

## 🧱 Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3, dynamic color)
- **CameraX** for capture
- **AndroidX Navigation Compose** for in-app routing
- **Android PdfDocument** for the print-ready PDF (no extra PDF library)
- **Custom Canvas / Path** rendering for vector annotations (no third-party drawing lib)
- **SensorManager** (rotation vector + accel + magnetometer) for the compass
- **Sobel + box-blur depth estimation** in pure Kotlin for the 2D→3D anaglyph (no ML, no native libs)
- **Accompanist Permissions** for runtime camera permission
- **AppCompatDelegate per-app locales** for English / বাংলা

---

## 📁 Project structure

```
FieldMark/
├── .github/workflows/build.yml      # CI: build debug + release APK, publish to Releases
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml         # Version catalog
├── app/
│   ├── build.gradle.kts
│   ├── src/main/AndroidManifest.xml
│   ├── src/main/res/
│   │   ├── values/strings.xml        # English
│   │   ├── values-bn/strings.xml     # বাংলা
│   │   ├── values/colors.xml, themes.xml
│   │   ├── xml/{file_paths,locales_config,backup_rules,data_extraction_rules}.xml
│   │   ├── drawable/ic_launcher_foreground.xml
│   │   └── mipmap-anydpi-v26/ic_launcher{,_round}.xml
│   └── src/main/kotlin/com/fieldmark/app/
│       ├── FieldMarkApp.kt           # Application (applies stored locale)
│       ├── MainActivity.kt           # Edge-to-edge, theme, navigation host
│       ├── nav/AppNav.kt
│       ├── ui/
│       │   ├── theme/                # Material 3 theme, dynamic color
│       │   ├── screens/              # Home, Camera, Editor, Compass, Anaglyph
│       │   └── components/           # AnnotationCanvas, CompassRose
│       ├── annotation/               # Annotation sealed model
│       ├── compass/                  # CompassRepository (rotation vector)
│       ├── stereo/AnaglyphGenerator  # Sobel depth + chromatic disparity
│       ├── export/PdfExporter        # Title block + image + A4 PDF
│       └── i18n/LocaleManager        # Per-app English/বাংলা switcher
```

---

## 🌍 Localization

The app supports two locales out of the box:

- **English** (`en`) — default
- **বাংলা** (`bn`) — full translation

Switch from the language icon in the top-right of the Home screen. The choice is persisted in `SharedPreferences` and applied per-app via `AppCompatDelegate.setApplicationLocales` — system language is **not** overridden.

To add a new language, copy `app/src/main/res/values/strings.xml` to `app/src/main/res/values-<code>/` and translate. Then add the locale tag to `app/src/main/res/xml/locales_config.xml`.

---

## 🧭 The 2D → 3D anaglyph algorithm

FieldMark converts any 2D photo into a red-cyan anaglyph that can be viewed with cheap 3D glasses (red on the left, cyan on the right).

1. Convert to luminance (Rec. 601 weights).
2. Estimate per-pixel depth using a **Sobel edge magnitude + high-pass luminance residual** (edges = closer to camera).
3. Build a horizontal **shift map** in pixels (configurable `maxShiftPx`, default 14).
4. Sample the red channel from the original; sample green/blue from a horizontally-shifted version.
5. Result: a single image where each eye sees a slightly different perspective, producing a depth illusion.

This is a **real 2D→3D transform** with no ML model, no native code, and no internet — it runs in milliseconds on a modern phone.

---

## 🗺 Roadmap (from the broader PRD vision)

FieldMark ships the **core engineering workflow** today. These are planned follow-ups:

- 📐 **Live electrical calculator** (cable sizing, voltage drop, PFC bank sizing)
- 🧾 **Money-receipt / billing suite** with company branding (logo, BIN/TIN)
- 📁 **CAD viewer** (`.dwg` / `.dxf` / blueprint PDF) with layer toggling
- 🤖 **AI object detection** for common electrical parts
- 🌡️ **Thermal camera** overlay (FLIR bridge)
- 🔳 **QR / barcode asset tracking**
- ☁️ **Optional cloud sync** for team collaboration

---

## 📜 License

MIT — do anything you want, just don't blame us if the measurement is off by 2 mm. Always double-check critical dimensions on-site.
