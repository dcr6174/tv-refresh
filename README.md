<div align="center">

# TV Refresh
### Less clutter. More TV.

A calm home screen for your TV, with practical guides for cleanup, browsers, DNS and screensavers.

**Native Android launcher · Remote friendly · Android 9+ · No root**

[Start here](docs/start-here.md) · [Get the APK](dist/TVRefresh-v1.0.apk) · [Screenshots](docs/screenshots.md) · [Undo changes](docs/restore.md)

<img src="docs/screenshots/home-current.png" alt="TV Refresh on an Android TV: five favorites in a low floating dock and a Dussehra wallpaper" width="100%">

*Real capture from an Android TV. Auto returns to a solid background between dated festivals.*

</div>

## What changes

Open your TV and go straight to your apps. Keep five favorites in one floating dock. Press Down to see everything else. Keep the original app logos, a clear focus highlight and a name you can read from the sofa.

| Before: original TV setup | After: TV Refresh |
| --- | --- |
| ![Original launcher during initial setup](docs/screenshots/stock-setup.png) | ![TV Refresh on the same TV](docs/screenshots/home-current.png) |

These are screenshots, not performance benchmarks. The before capture includes the TV's developer-settings panel.

## Start in six steps

| Step | Do this | What you should see |
| --- | --- | --- |
| **1** | Put your computer and TV on the same Wi-Fi. | Both are connected. |
| **2** | Turn on TV debugging. | Developer options and a debugging switch. |
| **3** | Connect your computer and choose **Allow** on the TV. | ADB reports `device`. |
| **4** | Save a backup. | A new backup file on your computer. |
| **5** | Install the APK and choose TV Refresh as Home. | The five-app dock. |
| **6** | Try Home, Back, OK and the arrows. | Apps open and navigation returns where expected. |

**[Follow every step, with commands →](docs/start-here.md)**

Cleanup and DNS are separate choices. Installing the launcher does not automatically debloat your TV or change DNS.

## What you get

| Home | Personalize | Useful extras |
| --- | --- | --- |
| Five favorites in a low floating dock | Hold OK to move, hide or replace an app | Inputs, Wi-Fi name, Settings and Themes |
| Original app banners and a soft focus glow | Five vivid colors plus Aurora | Clock with seconds and bold day/date |
| Down opens All Apps immediately | Telugu festival themes and manual selection | Weather integration in source |
| Back restores the All Apps position | Auto returns to a solid background between festivals | Auto screensavers and drifting clock |
| Home returns to the favorites dock | Brief festival effects and Deepavali fireworks | Fan cleanup with genie-like wisps |

The fan trims eligible temporary caches and closes eligible hidden app tasks without force-stop. Active media and essential services are protected. Android may retain background services and historical recent entries. The animation is decorative, not a RAM meter.

## A closer look

| All Apps | Customize |
| --- | --- |
| ![All Apps from an earlier development build](docs/screenshots/all-apps-earlier.png) | ![Customization from an earlier development build](docs/screenshots/customize-earlier.png) |
| Down opens the grid immediately. | Hold OK, release it, then choose an action. |

![Drifting clock screensaver](docs/screenshots/clock.png)

*The gallery includes earlier development captures, labeled as such. Controls, app order and decoration may differ from v1.0.*

## Supported devices

For **Android TV and Google TV devices running Android 9 or later**.

Compatibility varies by device and firmware. The included native Inputs integration and HDMI 2 shortcut are vendor-specific; adapt them for your TV. Weather location, app packages and task cleanup also need review. See [device setup](examples/device-setup/README.md). Samsung Tizen and LG webOS do not run this Android APK.

## Pick a guide

- [First installation: every step](docs/start-here.md)
- [Backup and restore](docs/restore.md)
- [Debloating: choose what to disable](docs/debloating.md)
- [Remote buttons and favorites](docs/launcher.md)
- [Themes and festival dates](docs/themes.md)
- [Screensavers, sleep and power](docs/screensavers.md)
- [AdGuard DNS](docs/dns.md)
- [Browser setup](docs/browser.md)
- [Troubleshooting](docs/troubleshooting.md)
- [Build from source](docs/build.md)

## Project layout

```text
launcher/           Native Java launcher and Windows build script
tools/              ADB inventory / reversible-disable helper
examples/device-setup/  Tested-device notes, not a universal debloat list
docs/               Setup guides and real TV screenshots
dist/               Versioned APK and SHA-256 checksum
tests/              Remote-confirmation and task-selection checks
```

## Permissions and data

No analytics SDK is included. Weather uses Open-Meteo; optional video screensavers use their own providers. The cleaner needs local ADB and generates its key in private app storage. No computer ADB key or signing key is published. Secure-settings permission enables screensaver and sleep configuration. Read [security and privacy](SECURITY.md).

## License

TV Refresh contributions are released under the [MIT License](LICENSE). Required third-party software and font notices are included in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

App names and logos belong to their owners; no affiliation is implied. The devotional wallpaper is generated artwork; its [prompt](wallpaper-generation.txt) is included.

[Contributions](CONTRIBUTING.md) and device reports are welcome.
