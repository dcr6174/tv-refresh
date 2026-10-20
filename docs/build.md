# Build from source

Use JDK 17, Android 34's `android.jar`, Android build tools with AAPT2 and apksigner, and Python 3. The script is for Windows PowerShell and builds native Java without Gradle or AndroidX.

From the repository root:

```powershell
./launcher/build.ps1 -Jdk "C:/path/to/jdk-17" -BuildTools "C:/path/to/build-tools" -AndroidJar "C:/path/to/android-34/android.jar" -Python python -Aapt2 "C:/path/to/aapt2.exe"
```

Output: `launcher/TVRefresh.apk`. The script generates a local `launcher/signing.keystore` if absent. Keep it private and retain it for updates. A different key cannot update the included APK directly; uninstalling deletes launcher preferences.

## Adapt to another TV

- `res/values/config.xml`: package names and grid columns.
- `MainActivity.java`: native TCL inputs and HDMI shortcut.
- `WeatherWidget.java`: location label and Open-Meteo coordinates. The original demonstration location is included.
- `RecentsCleaner.java`: conservative task-cleanup allowlist.
- `assets/festivals.json`: dated festival windows.

The application ID stays unchanged for update compatibility. Its visible name is TV Refresh.

## Run focused checks

```sh
javac -d work-tests launcher/src/com/chand/minimalhome/ConfirmGate.java tests/com/chand/minimalhome/ConfirmGateCheck.java launcher/src/com/chand/minimalhome/RecentsCleaner.java tests/RecentsCleanerCheck.java
java -cp work-tests com.chand.minimalhome.ConfirmGateCheck
java -cp work-tests com.chand.minimalhome.RecentsCleanerCheck
python tools/check-project.py
```

These check confirmation events, task-selection guards, local documentation links and accidental credential files. Vendor input, casting and remote behavior need hardware checks.
