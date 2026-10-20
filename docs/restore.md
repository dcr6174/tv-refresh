# Undo changes

Use the same TV and the snapshot saved before setup.

## 1. Restore the original Home

Find the original Home component under `home` in `backups/tv-state.json`. Replace the placeholders below. If you disabled its package, enable it first:

```sh
adb -s TV_IP:5555 shell pm enable --user 0 ORIGINAL_HOME_PACKAGE
adb -s TV_IP:5555 shell am force-stop com.chand.minimalhome
adb -s TV_IP:5555 shell cmd package set-home-activity --user 0 ORIGINAL_HOME_COMPONENT
```

Press Home. **Check:** the original launcher appears. The force-stop here stops TV Refresh's scheduler during recovery; its normal fan cleanup does not force-stop apps.

## 2. Restore recorded package changes

```sh
python tools/tv-refresh.py --serial TV_IP:5555 restore
```

**Check:** the recorded packages return to their original enabled states. This command restores only package changes made by the helper.

## 3. Restore changed settings

Use the snapshot's `settings` values:

```sh
adb -s TV_IP:5555 shell settings put SCOPE KEY ORIGINAL_VALUE
```

If the saved value is `null`, remove the override:

```sh
adb -s TV_IP:5555 shell settings delete SCOPE KEY
```

Relevant keys include `global/private_dns_mode`, `global/private_dns_specifier`, `secure/screensaver_components`, `secure/screensaver_enabled`, `secure/screensaver_activate_on_sleep`, `secure/sleep_timeout` and `system/screen_off_timeout`. Restore your values, not another TV's.

## 4. Remove TV Refresh if desired

After checking that stock Home works:

```sh
adb -s TV_IP:5555 uninstall com.chand.minimalhome
```

This deletes launcher preferences and its private cleanup key. Streaming apps remain installed.

A preinstalled app removed for user 0 needs `adb shell cmd package install-existing --user 0 PACKAGE_NAME`. Enabled-state restore alone cannot reinstall it or recover deleted app data.
