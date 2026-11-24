# PM SHRI KV NDA, PUNE — Income Tax Calculator (Android App)

[![Android CI/CD](https://github.com/aryakadam07/Google-Apps-Script-Android-App/actions/workflows/android.yml/badge.svg)](https://github.com/aryakadam07/Google-Apps-Script-Android-App/actions/workflows/android.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20%28API%2024%2B%29-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2%2B-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Proprietary%20%2F%20Internal-lightgrey.svg)](LICENSE)

A production-grade, native Android application for **PM SHRI Kendriya Vidyalaya NDA, Pune**, converting the official **Google Apps Script Web App** for Employee Income Tax Calculation (GPF / NPS) into a complete, standalone mobile application.

---

## 📱 Web Application Source & Target

* **Original Web Application URL:**
  [`https://script.google.com/macros/s/AKfycbwik4YejYW3GlAQYdwcDnI0Vlu2iwGXsnCBTNTiqck1oFgxbsj9HV6MRthd6rx_lmqZXQ/exec`](https://script.google.com/macros/s/AKfycbwik4YejYW3GlAQYdwcDnI0Vlu2iwGXsnCBTNTiqck1oFgxbsj9HV6MRthd6rx_lmqZXQ/exec)
* **Application ID / Package:** `com.pmshri.kvnda.taxcalc`
* **Target Audience:** KV NDA Employees, Faculty, and Accounts Section for Financial Year 2026–27 Tax Estimation.

---

## 🌟 Key Application Features

1. **Full Fidelity Google Apps Script Execution:**
   * Embedded high-performance WebView with complete DOM storage, JavaScript execution, and secure cross-domain cookie handling for `script.google.com` and `script.googleusercontent.com`.
   * Preserves the full two-step Gmail OTP authentication flow (`requestOtp`, `verifyOtp`) and spreadsheet data retrieval (`getTaxData`).
   * No backend changes required; zero dummy data.

2. **Native Android PDF Export & Print Integration:**
   * In the original web application, the *Download PDF* button triggers `window.print()`, which is unsupported in standard Android WebViews.
   * This app features a native JavaScript interface (`WebAppInterface`) that bridges `window.print()` directly to Android's native **`PrintManager`** and `PrintDocumentAdapter`.
   * Tap "Download PDF" or use the top-right toolbar action to immediately open Android's system Print preview and save as an A4 Landscape PDF.

3. **Active Network Connectivity Detection:**
   * Uses modern `ConnectivityManager.NetworkCallback` (API 24+) to monitor internet connection changes in real-time.
   * Displays an attractive offline screen with a dedicated "Retry Connection" button when no internet is available.
   * Automatically attempts reconnection as soon as the device regains mobile data or Wi-Fi.

4. **Pull-to-Refresh & Smooth Loading:**
   * Built-in `SwipeRefreshLayout` lets users pull down at any time to refresh the calculation.
   * Sleek progress bar below the header reflects actual page loading state without flickering.

5. **Intuitive Back Navigation:**
   * Integrated with AndroidX `OnBackPressedDispatcher`.
   * Pressing the back button navigates backward through the WebView's browsing history; pressing back on the root screen prompts a clean confirmation dialog to prevent accidental app closure.

6. **Enterprise Security Standards:**
   * `cleartextTrafficPermitted="false"` enforced via `network_security_config.xml` (HTTPS only).
   * Strict SSL certificate validation: never bypasses SSL errors.
   * File access from web contents is locked down (`allowFileAccess = false`).
   * Zero API keys or secrets stored in client-side code.

---

## 🏗️ Architecture & Project Structure

The project follows standard Android Studio and Kotlin architectural best practices:

```
Google-Apps-Script-Android-App/
├── .github/
│   └── workflows/
│       └── android.yml            # Automated CI/CD (Builds APKs, AAB, & GitHub Releases)
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/pmshri/kvnda/taxcalc/
│   │   │   │   ├── MainActivity.kt        # Main Activity, UI orchestration, and print bridge
│   │   │   │   ├── WebViewManager.kt      # WebView settings, clients, redirects & security
│   │   │   │   ├── NetworkManager.kt      # Real-time network detection & callbacks
│   │   │   │   └── WebAppInterface.kt     # JavaScript-to-Kotlin print bridge
│   │   │   ├── res/
│   │   │   │   ├── drawable/              # Vector icons (school logo, print, refresh, no-internet)
│   │   │   │   ├── layout/
│   │   │   │   │   └── activity_main.xml  # Toolbar, progress bar, WebView & offline error card
│   │   │   │   ├── values/
│   │   │   │   │   ├── colors.xml         # KV Blue & brand color palette
│   │   │   │   │   ├── strings.xml        # User-facing strings
│   │   │   │   │   └── themes.xml         # Material3 theme & splash styling
│   │   │   │   └── xml/
│   │   │   │       └── network_security_config.xml
│   │   │   └── AndroidManifest.xml
│   │   └── test/java/com/pmshri/kvnda/taxcalc/
│   │       └── WebViewManagerTest.kt      # Unit tests
│   ├── build.gradle.kts                   # App-level build script & signing config
│   └── proguard-rules.pro
├── gradle/
│   ├── libs.versions.toml                 # Version catalog
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── keystore/                              # Keystore directory (git-ignored)
├── release_outputs/                       # Pre-built installable APK and AAB files
│   ├── KV_NDA_Tax_Calculator_debug.apk
│   ├── KV_NDA_Tax_Calculator_release.apk
│   └── KV_NDA_Tax_Calculator_release.aab
├── build.gradle.kts                       # Root build configuration
├── settings.gradle.kts                    # Project settings & repositories
├── gradle.properties                      # JVM args & AndroidX flags
├── .gitignore                             # Android & signing key protection
└── README.md
```

---

## 📦 Build Outputs & Deliverables

Pre-built binaries are ready in `release_outputs/`:

| Artifact | File Name | Size | Purpose |
|---|---|---|---|
| **Debug APK** | `KV_NDA_Tax_Calculator_debug.apk` | ~5.8 MB | Testing with USB debugging & logcat |
| **Release APK** | `KV_NDA_Tax_Calculator_release.apk` | ~4.7 MB | **Direct installation on any Android phone** |
| **App Bundle** | `KV_NDA_Tax_Calculator_release.aab` | ~4.2 MB | Publishing to Google Play Console |

---

## 🛠️ How to Build from Source

### Prerequisites
* **Android Studio Ladybug (2024.2+)** or command line with **Android SDK (Platform 35)**.
* **JDK 17 or JDK 21+**.

### Build Commands (Command Line)

1. **Clone the repository:**
   ```bash
   git clone https://github.com/aryakadam07/Google-Apps-Script-Android-App.git
   cd Google-Apps-Script-Android-App
   ```

2. **Generate Debug APK:**
   ```bash
   ./gradlew assembleDebug
   # Output: app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Generate Signed Release APK:**
   ```bash
   ./gradlew assembleRelease
   # Output: app/build/outputs/apk/release/app-release.apk
   ```

4. **Generate Release Android App Bundle (AAB):**
   ```bash
   ./gradlew bundleRelease
   # Output: app/build/outputs/bundle/release/app-release.aab
   ```

5. **Run Unit Tests:**
   ```bash
   ./gradlew test
   ```

---

## 🔑 Release Keystore Setup & Management

For local development and testing, a 2048-bit RSA release keystore is configured. In production, never commit `.jks` files or passwords to GitHub.

### Creating a New Production Keystore
```bash
keytool -genkeypair -v \
  -keystore keystore/release.jks \
  -alias kvnda \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "YOUR_SECURE_PASSWORD" \
  -keypass "YOUR_SECURE_PASSWORD" \
  -dname "CN=PM SHRI KV NDA, OU=Accounts, O=Kendriya Vidyalaya NDA, L=Pune, ST=Maharashtra, C=IN"
```

### Passing Passwords via Environment Variables
Set the following environment variables on your CI/CD runner or local terminal:
```bash
export KEYSTORE_PASSWORD="your_keystore_password"
export KEY_ALIAS="kvnda"
export KEY_PASSWORD="your_key_password"
```

---

## 📲 How to Install the APK on Your Android Device

### Method 1: Direct File Transfer (USB / WhatsApp / Drive)
1. Copy `KV_NDA_Tax_Calculator_release.apk` to your phone.
2. Open the **Files** app or tap the downloaded APK notification.
3. If prompted with *"For your security, your phone is not allowed to install unknown apps from this source"*:
   * Tap **Settings**.
   * Toggle on **Allow from this source**.
4. Tap **Install**, then tap **Open**.

### Method 2: Command Line (via ADB)
Connect your phone with USB debugging enabled and run:
```bash
adb install -r release_outputs/KV_NDA_Tax_Calculator_release.apk
```

---

## 📊 Distribution Methods Explained

### 1. Direct APK Installation
* **Format:** `.apk`
* **How it works:** The package contains all CPU architectures (arm64, armeabi-v7a, x86_64) and resources bundled together.
* **Best for:** Immediate internal testing, direct sideloading, or distribution to school staff without using app stores.

### 2. Sharing the APK with Colleagues
* **How it works:** Share the `KV_NDA_Tax_Calculator_release.apk` file via Google Drive, WhatsApp, Email, or an internal portal.
* **Recipient experience:** Recipients tap the file, allow installation from their browser/file manager, and the app is installed. Updates must be distributed manually by sharing a new APK with a higher `versionCode`.

### 3. Publishing the Android App Bundle (AAB) to Google Play
* **Format:** `.aab`
* **How it works:** You upload `KV_NDA_Tax_Calculator_release.aab` to the [Google Play Console](https://play.google.com/console). Google uses Dynamic Delivery to generate optimized APKs specifically tailored to each user's device screen density and CPU architecture (reducing download size by 30–50%).
* **Benefits:** Automatic updates, Play Protect verification, and no "unknown sources" security warning.

---

## 🔄 Instructions for Future Updates

When updating the application in the future:
1. **Update Version Numbers** in `app/build.gradle.kts`:
   ```kotlin
   defaultConfig {
       versionCode = 2       // Increment integer for each release
       versionName = "1.1.0" // Semantic version string
   }
   ```
2. **Commit and Tag**:
   ```bash
   git add .
   git commit -m "Release v1.1.0: Updated tax calculation rules"
   git tag v1.1.0
   git push origin main --tags
   ```
3. GitHub Actions will automatically trigger, build the new release APK/AAB, and attach them to a new GitHub Release.
