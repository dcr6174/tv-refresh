# Fix a problem

| What you see | What to do |
| --- | --- |
| `adb` is not recognized | Open Platform Tools or add it to PATH. |
| `unauthorized` | Select Allow on the TV's debugging prompt. |
| Connection refused | Recheck IP, port and debugging. Some TVs need initial USB setup; newer wireless debugging may need `adb pair IP:PAIRING_PORT`. |
| APK will not install | Check Android 9+, storage and the exact error. Updates require the same signing key. |
| Home opens the old launcher | Repeat set-home and check the default Home app. Keep stock Home installed until the new one works. |
| Remote behaves strangely | Stop computer scripts sending key events. Test the physical remote alone. Report the exact screen and button. |
| Long-press menu closes | Use v1.0+, hold OK then release, and press OK again to choose. |
| Back stays inside an app | Apps control internal Back behavior. Exit the app first; Home returns to the dock. |
| Live TV opens the wrong source | The shortcut is HDMI 2 on the example TV. Adapt the URI for your model. |
| Fan cannot clean | Local ADB at 127.0.0.1:5555 must be available. Approve its prompt if shown. No force-stop fallback is used. |
| Apps remain after cleanup | Active media and essential services are protected. Services and old history can remain. |
| Aerial screenshot is black | Look at the TV; hardware video may not appear in screenshots. |
| Internet fails after DNS setup | Restore the previous Private DNS values. |

For a report, include TV model, Android version, app version, action and result. Remove IP addresses, Wi-Fi names and account information from public logs.
