# YT Auto

YouTube on the Android Auto screen, and nothing else. A stripped-down take on the YouTube addon
from [Fermata](https://github.com/AndreyPavlenko/Fermata): it opens straight to `m.youtube.com`,
with no media library, addons or settings.

[![Download APK](https://img.shields.io/github/v/release/Pseudoman21/ytauto?label=Download%20APK&logo=android&style=for-the-badge)](https://github.com/Pseudoman21/ytauto/releases/latest)
[![YouTube](https://img.shields.io/badge/YouTube-Kasangga%20Vlogs-FF0000?logo=youtube&style=for-the-badge)](https://www.youtube.com/@KasanggaVlogs)

## Download

Get the latest APK from the **[Releases page](https://github.com/Pseudoman21/ytauto/releases/latest)**
(open it on your phone, tap `YTAuto-v….apk` under **Assets**), then follow
[Install and enable in Android Auto](#install-and-enable-in-android-auto) below.

⚠️ **Please drive safe.** Keep your eyes on the road and only watch while parked.

## How it works

- `YtWebView` loads m.youtube.com and injects a script that reports the `<video>` state
  (playing, paused, ended, position, title) through a JS bridge, skips ads, and lets Java drive
  play, pause, seek and next/prev.
- `Player` mirrors that state into a MediaSession, so steering wheel and media buttons work.
  In the car, a video goes fullscreen when it starts.
- `car/YtCarAppService` is the Android Auto entry point. It's a navigation app built on the
  official Car App Library, because newer Android Auto versions (17.x) only list sideloaded apps
  that use that library. A navigation app gets a raw drawing surface on the head unit:
  `SurfaceRenderer` puts a private virtual display on that surface, shows the YouTube page on it,
  and replays the car's taps and drags as touch events. `PlayerScreen` adds Back/Home buttons,
  and `SearchScreen` is the car keyboard for the page's search box.
- `car/CarService` + `car/CarMainActivity` are the older entry point, using the unofficial
  Android Auto SDK (`app/libs/aauto.aar`, from Fermata). Older Android Auto versions may still list
  it; newer ones hide it.
- `PhoneActivity` is the same page on the phone. Use it to sign in to YouTube, since the car
  shares its cookies.

## Build

```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools   # or your SDK path
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk (signed with the debug key)
```

## Install and enable in Android Auto

Android Auto hides apps that don't come from the Play Store, so installing YT Auto takes a few
more steps than a normal APK. These steps are tested with Android Auto 17.8.

### 1. Turn on Android Auto's developer settings

1. On the phone, open **Android Auto settings** and tap **Version** about 10 times until it says
   developer settings are enabled.
2. Tap **⋮** (top right) → **Developer settings** → tick **Unknown sources**.

### 2. Install the APK as if it came from the Play Store (recommended)

Android Auto may skip apps installed by tapping the APK. Installing with `adb` and naming the Play
Store as the installer avoids that. You need a computer with
[platform-tools](https://developer.android.com/tools/releases/platform-tools) (`adb`) and a USB cable.

1. Download `YTAuto-v….apk` from [Releases](https://github.com/Pseudoman21/ytauto/releases/latest)
   to the computer.
2. On the phone, enable **Developer options** (Settings → About phone → tap **Build number** 7
   times), then turn on **USB debugging**.
   - Xiaomi/Redmi (MIUI/HyperOS): also turn on **Install via USB** in Developer options. Without
     it, `adb install` fails with `INSTALL_FAILED_USER_RESTRICTED`.
3. Connect the phone, accept the "Allow USB debugging?" prompt, and run:

   ```bash
   adb install -r -i com.android.vending YTAuto-v1.2.apk
   ```

   `-r` keeps your sign-in when updating. `-i com.android.vending` records the Play Store as the
   installer. Use this same command for every update: installing a newer APK by tapping it on the
   phone resets the installer.

**No computer?** Download the APK on the phone and tap it to install (allow installing from your
browser if asked). This is the simplest option, but if YT Auto doesn't show up in step 3, reinstall
it with `adb` as above.

### 3. Sign in and turn it on

1. Open **YT Auto** on the phone once and sign in to YouTube (optional; the car shares the
   phone's cookies).
2. **Android Auto settings → Customise launcher**: make sure **YT Auto** is ticked.
3. Connect to the car. YT Auto appears in the car's app launcher. It's listed as a navigation app,
   so it may sit with the map apps.

In the car, tap to click and drag to scroll. **Back** and **Home** are in the top corner, and
tapping YouTube's search box opens the car keyboard. Steering wheel media buttons control playback.

## Test without a car (Desktop Head Unit)

Google's Desktop Head Unit (DHU) shows Android Auto in a window on your computer, using your
phone over USB.

1. Install it: `sdkmanager "extras;google;auto"` (or SDK Manager → SDK Tools → **Android Auto
   Desktop Head Unit Emulator** in Android Studio).
2. On the phone: **Android Auto settings → ⋮ → Start head unit server**. Keep the phone unlocked.
3. On the computer:

   ```bash
   adb forward tcp:5277 tcp:5277
   cd "$ANDROID_HOME/extras/google/auto" && ./desktop-head-unit
   ```

4. Open the app grid in the DHU window and click **YT Auto**. Mouse clicks are taps, and
   click-and-drag scrolls.

If the window stays on "Waiting for phone", the phone's head unit server is stuck: close the DHU,
force-stop Android Auto (or **⋮ → Stop head unit server**), start the server again and rerun step 3.
