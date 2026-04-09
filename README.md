# Race Day Countdown - Karoo Extension

A countdown timer extension for Hammerhead Karoo K2/K3 cycling computers. Set your race start time, and the data field counts down with audible alerts — then auto-starts your ride recording at zero.

![IMG_3333](https://github.com/user-attachments/assets/c59a75cc-350a-4c68-bf9a-529b6125ee50)
![IMG_3344](https://github.com/user-attachments/assets/c4c251ca-4aa5-4430-9f37-2ae9342bb97f)



- **Countdown timer** on your ride screen — big bold monospace numbers
- **Color-coded urgency** — white normally, yellow under 5 min, red under 1 min
- **Beep + flash alerts** at 10 min, 5 min, and 1 min before race start
- **"RACE ON!" flash** at zero with triple beep
- **Auto-starts ride recording** when the countdown hits zero
- **Switches to live clock** after race starts — no wasted data field
- **Survives power cycles** — race time saved in SharedPreferences
- **Auto-clears** the saved time after race starts so it's clean for next time

## How It Works

1. Open the app from the Extensions folder on your Karoo
2. Tap "Set Race Time" — pick date and time
3. Close the app
4. Add the "Race Countdown" data field to your ride profile
5. At the start line, your ride screen shows the countdown ticking
6. Beeps alert you at 10, 5, and 1 minute
7. At zero: beep + flash + ride auto-starts
8. Data field switches to a regular clock for the rest of your ride

## Setup Flow (Race Day)

- **Night before:** Open app, set race time, close it
- **Morning of:** Power on Karoo, go to your ride screen — countdown is already running
- **At the line:** Watch it count down, hands on bars
- **Gun goes off:** "RACE ON!" flash, ride starts recording automatically

## Technical Details

- Built on **karoo-ext 1.1.8** SDK
- Same `startStream()` / `startView()` architecture as [Karoo Trail Names](https://github.com/robrusk/Karoo-Trail-Names)
- `PlayBeepPattern` for hardware buzzer alerts
- `PerformHardwareAction.BottomRightPress` for auto-start ride
- `RemoteViews` graphical data field with 500ms polling
- Compatible with **Karoo 2** and **Karoo 3**

## Build

Requires Android Studio with:
- AGP 9.0.1+
- Kotlin 2.0.21+
- karoo-ext 1.1.8 (GitHub Packages — requires authentication)

```
git clone https://github.com/robrusk/Race-Day-Countdown.git
```

Open in Android Studio, sync, build, sideload to Karoo via ADB.

## Install

Download the APK from [Releases](https://github.com/robrusk/Race-Day-Countdown/releases) and sideload to your Karoo:

```
adb install app-debug.apk
```

## Author

**Rob Rusk** — [Rusk Racing](https://ruskracing.com)

## License

MIT
