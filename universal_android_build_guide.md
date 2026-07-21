# Panduan Utama Kompilasi (CI/CD) Aplikasi Android dengan GitHub Actions

Dokumen ini adalah panduan lengkap dan universal tentang cara membangun (*build*), mengamankan, mengoptimalkan, dan menandatangani (*sign*) **aplikasi Android apa pun** (bukan hanya yang menggunakan FFmpeg) menjadi file **`.apk`** atau **`.aab`** menggunakan **GitHub Actions**.

---

## 🛠️ 1. Template Workflow Universal (`android-universal-build.yml`)

Berikut adalah template alur kerja (*workflow*) standar industri terbaru yang dapat digunakan untuk **semua proyek Android berbasis Java, Kotlin, Flutter, atau React Native**:

```yaml
name: Universal Android Build Pipeline

on:
  push:
    branches: [ "main", "master" ]
  pull_request:
    branches: [ "main", "master" ]
  workflow_dispatch: # Mengizinkan kompilasi manual sekali klik dari web GitHub

jobs:
  build:
    name: Build & Package Android App
    runs-on: ubuntu-latest

    steps:
      # 1. Mengunduh kode sumber proyek dari repositori
      - name: Checkout Repository
        uses: actions/checkout@v4

      # 2. Memasang Java Development Kit (JDK) - Sesuaikan versi dengan kebutuhan proyek (JDK 17 adalah standar modern)
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      # 3. Mengatur cache Gradle untuk menghemat waktu build hingga 80% pada proses berikutnya
      - name: Setup Gradle Cache
        uses: gradle/actions/setup-gradle@v4
        with:
          cache-disabled: false

      # 4. Memastikan file eksekusi Gradle Wrapper memiliki izin akses penuh
      - name: Grant Execute Permission for Gradle Wrapper
        run: |
          if [ -f "gradlew" ]; then
            chmod +x gradlew
          else
            # Jika gradlew hilang, buat wrapper baru secara otomatis
            gradle wrapper --gradle-version 8.2
            chmod +x gradlew
          fi

      # 5. Menjalankan proses kompilasi APK Debug (Tanpa Tanda Tangan Digital)
      - name: Build Debug APK
        run: ./gradlew assembleDebug --no-daemon

      # 6. Menjalankan proses kompilasi AAB / APK Release (Opsional, buka jika dibutuhkan)
      # - name: Build Release APK
      #   run: ./gradlew assembleRelease --no-daemon

      # 7. Mengunggah hasil APK agar bisa diunduh oleh pengguna di HP mereka
      - name: Upload Finished APK
        uses: actions/upload-artifact@v4
        with:
          name: Android-App-Debug-APK
          path: app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔑 2. Cara Membuat Release APK yang Ditandatangani (Signing Keystore)

Untuk merilis aplikasi ke Google Play Store atau memasangnya sebagai aplikasi resmi (*non-debug*), APK harus ditandatangani secara digital menggunakan file **Keystore (`.jks` / `.keystore`)**.

### Langkah Keamanan Menyimpan Keystore di GitHub Secrets:
1. **Ubah file Keystore Anda menjadi teks Base64** agar bisa disimpan dengan aman di GitHub. Jalankan perintah ini di terminal komputer Anda:
   ```bash
   openssl base64 -A -in my-release-key.jks -out keystore-base64.txt
   ```
2. Salin isi teks dari `keystore-base64.txt`.
3. Masuk ke halaman repositori GitHub Anda, buka **Settings > Secrets and variables > Actions > New repository secret**.
4. Buat rahasia (*secrets*) berikut:
   * `SIGNING_KEY` : Tempel teks Base64 tadi di sini.
   * `KEYSTORE_PASSWORD` : Kata sandi Keystore Anda.
   * `KEY_ALIAS` : Nama alias kunci Anda.
   * `KEY_PASSWORD` : Kata sandi alias kunci Anda.

### Tambahkan Langkah Ini di Workflow Anda sebelum mengunggah APK:
```yaml
      - name: Decode Keystore File
        run: echo "${{ secrets.SIGNING_KEY }}" | base64 --decode > app/release-key.jks

      - name: Sign Release APK
        run: |
          ./gradlew assembleRelease \
            -Pandroid.injected.signing.store.file=release-key.jks \
            -Pandroid.injected.signing.store.password=${{ secrets.KEYSTORE_PASSWORD }} \
            -Pandroid.injected.signing.key.alias=${{ secrets.KEY_ALIAS }} \
            -Pandroid.injected.signing.key.password=${{ secrets.KEY_PASSWORD }} \
            --no-daemon
```

---

## ⚡ 3. Tabel Kompatibilitas Versi Android (Wajib Diketahui!)

Kesalahan terbesar yang sering dilakukan oleh pengembang (dan AI) yang menyebabkan error build adalah **ketidakcocokan antara versi Java (JDK), Android Gradle Plugin (AGP), dan Gradle**. 

Gunakan tabel referensi berikut sebagai acuan:

| Target SDK Android | Versi JDK yang Direkomendasikan | Versi Android Gradle Plugin (AGP) | Versi Gradle yang Sesuai |
| :---: | :---: | :---: | :---: |
| **Android 14 (API 34)** | JDK 17 atau 21 | `8.1.x` s.d. `8.5.x` | Gradle `8.0` s.d. `8.7` |
| **Android 13 (API 33)** | JDK 11 atau 17 | `7.4.x` s.d. `8.0.x` | Gradle `7.5` s.d. `8.0` |
| **Android 12 (API 31/32)** | JDK 11 | `7.0.x` s.d. `7.3.x` | Gradle `7.0` s.d. `7.4` |
| **Android 11 (API 30)** | JDK 8 atau 11 | `4.1.x` s.d. `4.2.x` | Gradle `6.5` s.d. `6.8` |

---

## 🐞 4. Kamus Penyelesaian Masalah (Troubleshooting Guide)

### Masalah A: `Execution failed for task ':app:checkDebugAarMetadata' / Dependency Not Found`
* **Gejala:** Server gagal mengunduh library pihak ketiga (AAR).
* **Solusi:** Pastikan repositori dideklarasikan dengan benar di `settings.gradle` (bukan hanya di `build.gradle` proyek untuk standar Gradle modern). Jika menggunakan library non-standar, pastikan alamat repositori pihak ketiga (seperti Jitpack, Appodeal, dll.) ditambahkan.

### Masalah B: `Out of Memory (OOM) / Process killed with exit code 137`
* **Gejala:** Server kehabisan RAM saat melakukan kompilasi file Java yang besar.
* **Solusi:**
  1. Tambahkan argumen `--no-daemon` pada setiap eksekusi perintah Gradle di workflow Anda.
  2. Batasi penggunaan RAM kompiler di file `gradle.properties` proyek Anda:
     ```properties
     org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
     ```

### Masalah C: `AAPT2 aapt2-x.y.x-linux.jar Daemon startup failed`
* **Gejala:** AAPT2 gagal mengolah gambar atau tata letak XML.
* **Solusi:** Ini biasanya disebabkan oleh folder cache yang rusak atau masalah arsitektur pada server runner. Menambahkan perintah berikut di file `gradle.properties` sering kali menyelesaikan masalah:
  ```properties
  android.useNewApkCreator=true
  ```

---
*Dokumen ini dirancang sebagai pustaka pengetahuan universal yang dapat diimpor langsung ke kecerdasan buatan (AI) apa pun untuk mempercepat pemecahan masalah build Android.*
