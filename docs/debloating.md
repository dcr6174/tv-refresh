# Remove clutter

Start with one app you recognize and do not use. Package names vary by manufacturer. This project does not provide a universal bulk-disable list.

## 1. Save the backup

Complete steps 1–4 in [setup](start-here.md) before changing packages.

## 2. Find the exact package

```sh
adb -s TV_IP:5555 shell pm list packages
```

Check candidates in the TV's app settings and with `adb shell dumpsys package PACKAGE_NAME`. Keep Wi-Fi, Bluetooth, remote control, inputs, casting, Google Play services, updates and a working original launcher.

## 3. Disable one reviewed app

Replace `PACKAGE_NAME` with the app you reviewed:

```sh
python tools/tv-refresh.py --serial TV_IP:5555 disable PACKAGE_NAME
```

The helper records its original enabled state before disabling it. Common system/input/Home packages are protected; those guards do not identify every essential vendor service.

## 4. Try the TV

Check Home, volume, inputs, casting and your streaming apps. If anything fails:

```sh
python tools/tv-refresh.py --serial TV_IP:5555 restore
```

## Disable versus uninstall

The helper disables apps; it does not uninstall them. On the example TV, **Works with Alexa** was separately removed for user 0 at the owner's request:

```sh
adb -s TV_IP:5555 shell pm uninstall --user 0 com.tcl.smartalexa
```

Use that only for this exact preinstalled package if you do not use it. To restore its preinstalled copy:

```sh
adb -s TV_IP:5555 shell cmd package install-existing --user 0 com.tcl.smartalexa
```

That restores the package, not deleted app data. See [device notes](../examples/device-setup/README.md).
