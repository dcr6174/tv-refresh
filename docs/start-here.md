# Set up TV Refresh

Do one step at a time. Keep the TV on while you work.

## 1. Get the files

Download [TVRefresh-v1.0.apk](../dist/TVRefresh-v1.0.apk). Install Google's [Android SDK Platform Tools](https://developer.android.com/tools/releases/platform-tools). Open a terminal in the folder containing `adb`, or add that folder to PATH.

**Check:** `adb version` prints an Android Debug Bridge version.

## 2. Turn on TV debugging

1. On the TV, open **Settings**.
2. Open **Device Preferences → About**. Names vary by model.
3. Select **Build** seven times.
4. Go back and open **Developer options**.
5. Turn on **USB debugging**. If there is a separate **Network debugging** switch, turn it on.

**Check:** the debugging switch is on.

![Developer options on the example device](screenshots/stock-setup.png)

The bootloader-unlock switch is unrelated; leave it unchanged.

## 3. Connect the computer

Find the TV's IP address in **Settings → Network**. Replace `TV_IP` below with it:

```sh
adb connect TV_IP:5555
```

Select **Allow** on the TV's debugging prompt. Then run:

```sh
adb devices
```

**Check:** the TV address is followed by `device`. If it says `unauthorized`, approve the prompt. Port 5555 applies to the tested TCL; newer wireless-debugging screens may show another port and require `adb pair` first. See [troubleshooting](troubleshooting.md).

## 4. Save a backup

Install Python 3 if it is not already available. Open a terminal in the extracted project folder. Replace `TV_IP` again:

```sh
python tools/tv-refresh.py --serial TV_IP:5555 snapshot
```

**Check:** `Snapshot saved` appears, with `backups/tv-state.json`. Keep it. This records Home, relevant settings and package inventory, not app accounts or app data.

## 5. Install the launcher

From the project folder:

```sh
adb -s TV_IP:5555 install -r dist/TVRefresh-v1.0.apk
```

**Check:** the result is `Success`.

Open it:

```sh
adb -s TV_IP:5555 shell am start -n com.chand.minimalhome/.MainActivity
```

**Check:** the new home screen appears. The displayed name is **TV Refresh**. Its original package ID remains unchanged so existing installations can update.

## 6. Make it Home

After checking that it opens:

```sh
adb -s TV_IP:5555 shell cmd package set-home-activity --user 0 com.chand.minimalhome/.MainActivity
```

Press **Home** on the remote. If asked, choose **TV Refresh → Always**.

**Check:** Home opens TV Refresh. Keep the original launcher installed as a recovery option.

## 7. Try the remote

1. Press Left or Right. The selected tile changes.
2. Press OK. The app opens.
3. Return with Back. The launcher appears once the app exits.
4. Press Down on the dock. All Apps opens immediately.
5. Open an app from All Apps and return with Back. Its tile and scroll position should return.
6. Press Home. The favorites dock appears.
7. Hold OK on a tile, then release. The customization menu stays open.

## 8. Choose optional extras

- [Screensavers](screensavers.md): grant secure-settings permission for automatic selection.
- [DNS filtering](dns.md): configure AdGuard DNS separately.
- [Cleanup](debloating.md): review unwanted packages before disabling them.
- [Themes](themes.md): choose Auto or a manual theme from the palette icon.

Stop here if the launcher is all you want. [Undo instructions](restore.md) are always available.
