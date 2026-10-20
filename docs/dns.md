# Set up AdGuard DNS

DNS filtering blocks known advertising and tracking domains. It does not reliably remove YouTube video ads or ads sharing a video's domain. See [AdGuard's official guide](https://adguard-dns.io/en/public-dns.html).

## 1. Save the old values

Take the project snapshot, or record both results:

```sh
adb -s TV_IP:5555 shell settings get global private_dns_mode
adb -s TV_IP:5555 shell settings get global private_dns_specifier
```

## 2. Set the provider

```sh
adb -s TV_IP:5555 shell settings put global private_dns_specifier dns.adguard-dns.com
adb -s TV_IP:5555 shell settings put global private_dns_mode hostname
```

## 3. Check it

```sh
adb -s TV_IP:5555 shell settings get global private_dns_mode
adb -s TV_IP:5555 shell settings get global private_dns_specifier
adb -s TV_IP:5555 shell dumpsys connectivity
```

Expect `hostname` and `dns.adguard-dns.com`. On the example TV, connectivity also reported `UsePrivateDns: true`. Settings alone do not prove every app uses this DNS; apps can use their own resolver, cache or VPN. Open ordinary websites and streaming apps to check they work.

Some TCL firmware hides Private DNS in the Wi-Fi screen. Router DNS fields can still show local addresses; those differ from Android Private DNS.

## 4. Undo it

Restore the saved values. If both were `null`:

```sh
adb -s TV_IP:5555 shell settings delete global private_dns_mode
adb -s TV_IP:5555 shell settings delete global private_dns_specifier
```

If internet access fails after the change, restore the prior configuration.
