# FengOS (Feng iOS on Droid) — Tahap 1 + 2

Launcher Android bergaya iOS: boot, setup ala iPhone, home screen, dock, widget,
App Library, Spotlight, mode goyang.

## Cara build APK (tanpa Android Studio)
1. Upload folder ini ke repo GitHub (private juga boleh).
2. Buka tab **Actions** -> workflow **Build APK** (jalan otomatis saat push).
3. Setelah hijau, unduh artifact **FengOS-apk** -> `app-debug.apk`.
4. Install di HP, tekan Home, pilih **FengOS** -> **Selalu**.

## Aset opsional (taruh sendiri di `app/src/main/assets/`)
- `fonts/sf_pro.ttf`  : font SF Pro milikmu
- `wallpaper.jpg`     : wallpaper (disarankan <= 1080x2400)
Tanpa file ini, app memakai font sistem + wallpaper gradien.

## Catatan
- Passcode di setup hanya tampilan, tidak mengunci HP.
- Belum ada: Control Center, notifikasi, app switcher, folder, widget cuaca (Tahap 3-4).
