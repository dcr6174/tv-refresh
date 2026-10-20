# Screensavers and power

## 1. Enable automatic selection

Save a snapshot first. After installing TV Refresh:

```sh
adb -s TV_IP:5555 shell pm grant com.chand.minimalhome android.permission.WRITE_SECURE_SETTINGS
```

This is optional for the basic launcher. With it, the included build enables screensavers and writes `sleep_timeout=-1`, disabling Android's automatic sleep timeout. Separate vendor sleep timers can still exist.

## 2. Pick a scene

Open launcher Settings, then Screensavers. Choose Auto, Aerial, Clock, Aurora, Stars or Festival. Aerial needs the separate [Aerial Views](https://github.com/theothernt/AerialViews) app. If it is absent, Clock is used.

## 3. Understand Auto

The TV's local time determines the scene:

| Hours | Scene |
| --- | --- |
| 07:00–17:00 | Aerial, with Clock fallback |
| 17:00–19:00 | Aurora |
| 19:00–21:00 | Stars |
| 21:00–22:00 | Festival on dated event days; otherwise Aurora |
| 22:00–07:00 | Drifting clock |

Auto selects the next idle screensaver, rather than playing over active video. It uses a non-wakeup alarm. The launcher has no continuously running animation service.

## 4. Check idle timing

The tested TV uses a five-minute idle timeout. Screen-off/idle timing and sleep are separate:

```sh
adb -s TV_IP:5555 shell settings get secure sleep_timeout
adb -s TV_IP:5555 shell settings get system screen_off_timeout
```

Use the TV's screensaver settings where available. Hardware video layers may be missing from ADB screenshots, so check aerial video directly on the TV. Use [restore](restore.md) and your saved values to undo power changes.
