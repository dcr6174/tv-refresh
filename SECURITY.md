# Security and privacy

The optional cleaner connects only to 127.0.0.1:5555 and generates its own RSA key in private app storage. It does not contain the developer computer's key. Authorized local ADB has shell privileges. Use debugging on a trusted network and revoke authorizations when no longer needed. Disabling local ADB disables fan cleanup.

Secure-settings permission is optional for basic Home. The included scheduler uses it to choose a screensaver, enable it and set the sleep timeout. Save those settings before granting it.

Weather requests go to Open-Meteo. Aerial Views and browsers make their own requests. The launcher contains no analytics SDK; Android and vendor firmware can have their own telemetry.

Do not attach private backups or full logs to public issues. Report credential exposure privately through the repository owner's GitHub profile, without posting the credential. This project does not advertise a monitored private security mailbox.
