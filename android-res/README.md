# App icon resources (copy into androidApp/src/main/res)

- `mipmap-anydpi-v26/ic_launcher*.xml`: adaptive icon (background + foreground + monochrome). minSdk ≥ 26, so no PNG mipmaps are needed. Delete the template's default `ic_launcher*` PNG/webp files.
- `drawable/ic_launcher_monochrome.xml`: themed icon (Android 13+).
- `drawable/ic_stat_sliver.xml`: notification small icon. Use it for EVERY notification (`setSmallIcon`).
- `values/themes_splash.xml`: splash screen. `Theme.Sliver` is your normal app theme.
- `play/sliver-play-512.png`: Play Console listing icon. Upload it as is.

Reference board: design/src/AppIcon.dc.html. Check on a device: a circle launcher, themed icons ON and OFF, and the splash screen in dark and light system mode (the splash is always dark).
