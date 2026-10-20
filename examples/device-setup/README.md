# Device setup example

| Item | Observed configuration |
| --- | --- |
| Platform | Android TV with vendor-specific input support |
| Android | 9 / API 28 |
| Captured UI | 1920 × 1080 |
| Launcher | TV Refresh 1.0; com.chand.minimalhome |
| Live TV | HDMI 2 using TCL's passthrough service |
| Other inputs | HDMI 1 and HDMI 3 available through native Inputs |
| Optional apps | TV Bro, Aerial Views and Stremio; installed separately |
| DNS | Optional AdGuard Private DNS; configured separately |

Earlier cleanup disabled selected vendor services and recommendation packages. This public project excludes the private TV backup and a bulk-disable command. Review each TV's packages and record its original states.

TCL T-Cast/MagiConnect and Alexa are separate from Chromecast. Retain Google Cast and required Google services when disabling duplicate features. Works with Alexa (`com.tcl.smartalexa`) was removed for user 0 at the owner's request; `install-existing` can restore the preinstalled copy.

Input activities and HDMI identifiers can vary by firmware. Do not assume this mapping applies to another TV.

Development checks included APK installation/signature, default Home resolution, physical long-press confirmation reported by the user, landscape video reported on the TV, Private DNS configuration, and background browser-task removal with its force-stopped flag remaining false. This is not exhaustive compatibility certification or a performance benchmark.
