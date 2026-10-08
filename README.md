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
- `car/CarService` + `car/CarMainActivity` use the unofficial Android Auto SDK
  (`app/libs/aauto.aar`, from Fermata) to put the page on the head unit. `CarEditText` sends the
  car keyboard's input into the page's search box.
- `PhoneActivity` is the same page on the phone. Use it to sign in to YouTube, since the car
  shares its cookies.

## Build

```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools   # or your SDK path
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk (signed with the debug key)
```

## Install and enable in Android Auto

1. Download the APK from [Releases](https://github.com/Pseudoman21/ytauto/releases/latest) on your phone and open it
   to install (allow installing from your browser if asked). Or `adb install` it from a computer.
2. Open **YT Auto** on the phone once and sign in to YouTube (optional).
3. Android Auto settings → tap **Version** about 10 times to unlock developer settings →
   ⋮ menu → **Developer settings** → enable **Unknown sources**.
4. Android Auto settings → **Customize launcher** → make sure **YT Auto** is checked.
5. Connect to the car. YT Auto appears in the launcher.

Android Auto only lists apps from unknown sources when step 3 is done. That's a limitation of
Android Auto, not something the app can get around.
