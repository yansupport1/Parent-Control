# Parent App — Dashboard Kontrol Orang Tua

Bagian **Parent App** dari sistem kontrol 2-aplikasi (Parent + Child).
Dibangun dengan Kotlin + Jetpack Compose + Firebase Realtime Database.

## Fitur yang sudah ada di kode ini

- Daftar semua perangkat anak yang terhubung (realtime)
- Pairing perangkat baru lewat 6-digit session code (auto-expire 5 menit)
- Detail per anak:
  - Lock / Unlock perangkat
  - Atur batas waktu harian (slider 15–600 menit)
  - Minta live screen monitoring (kirim command ke child app)
  - Blokir/izinkan aplikasi sosmed satuan (Instagram, TikTok, WhatsApp, dst)
  - Hapus/putuskan perangkat

## Yang BELUM ada di sini (perlu dibuat terpisah)

Ini murni Parent App — **bukan** Child App. Supaya sistem lengkap sesuai
kebutuhanmu (limit pemakaian, auto-lock, live screen, app blocking di HP anak),
kamu masih perlu Child App terpisah yang berisi:

- `UsageStatsManager` untuk hitung durasi pemakaian per app
- `AccessibilityService` untuk deteksi app aktif & force-close saat diblokir
- `MediaProjection API` untuk live screen (Android **mewajibkan** notifikasi
  persisten ke pengguna selama sesi ini aktif — ini bukan pilihan, tapi
  ketentuan sistem operasi)
- `DeviceAdminReceiver` supaya app tidak gampang di-uninstall anak
- Listener Firebase yang baca node `/devices/{childId}/commands`

Saya bisa bantu buatkan Child App ini juga kalau kamu mau lanjut.

## Setup sebelum build

### 1. Firebase

1. Buat project di [Firebase Console](https://console.firebase.google.com)
2. Aktifkan **Authentication** (minimal Email/Password) dan **Realtime Database**
3. Set rules Realtime Database (contoh dasar, sesuaikan lagi untuk production):

```json
{
  "rules": {
    "parents": {
      "$parentId": {
        ".read": "auth.uid === $parentId",
        ".write": "auth.uid === $parentId"
      }
    },
    "devices": {
      "$childId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "pairing_sessions": {
      "$code": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
```

4. Download `google-services.json` dari Firebase Console (Project Settings →
   Your apps → Android app dengan package `com.parentalcontrol.parent`)
5. Taruh file itu di `app/google-services.json` (JANGAN commit ke repo publik)

### 2. Build lokal (Android Studio)

1. Buka folder ini di Android Studio (Hedgehog atau lebih baru)
2. Biarkan Gradle sync otomatis download dependency
3. Run ke emulator/device

### 3. Build otomatis lewat GitHub Actions

Workflow sudah disiapkan di `.github/workflows/build.yml`. Langkah:

1. Push project ini ke repo GitHub kamu
2. Encode `google-services.json` ke base64:
   ```bash
   base64 -i app/google-services.json | tr -d '\n' > encoded.txt
   ```
3. Buka repo → Settings → Secrets and variables → Actions → New repository secret
   - Name: `GOOGLE_SERVICES_JSON`
   - Value: isi dari `encoded.txt`
4. Push ke branch `main` → Actions tab akan otomatis build APK
5. APK hasil build bisa diunduh di tab **Actions → run terakhir → Artifacts**

## Catatan jujur soal "anti error"

Kode ini sudah saya susun mengikuti pola Android/Firebase yang standar dan
seharusnya build tanpa error struktural. Tapi saya tidak bisa menjamin 100%
bebas error tanpa benar-benar mengompilasinya di environment Android Studio —
ada faktor seperti versi SDK/Gradle di mesinmu, konfigurasi `google-services.json`
yang harus valid, dan Firebase rules yang harus cocok dengan skema di atas.
Kalau ada error saat build, kirim pesan errornya ke saya dan saya bantu perbaiki.

## Publikasi ke Play Store

Sebelum submit, wajib dipenuhi:
- Privacy Policy yang menjelaskan monitoring & data yang dikumpulkan
- Disclosure jelas ke pengguna (anak) bahwa app ini terpasang & aktif
- Kategori app: **Parenting** — App yang match kategori "Parental Control"
  dengan disclosure transparan sesuai [Google Play Families
  Policy](https://support.google.com/googleplay/android-developer/answer/9878809)
