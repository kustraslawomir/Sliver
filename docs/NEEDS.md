# External needs (fallbacks used)
| Need | Used by | Fallback |
|---|---|---|
| Google OAuth Web client ID (`GOOGLE_WEB_CLIENT_ID` in `.env.local`/local.properties) + SHA-1 registered in Google Cloud | T33-T37 | `GoogleAuth` interface with fake impl; Drive client behind interface with local fake |
| Release keystore | release builds | debug signing only |
| Emulator/device for CameraX/Media3 | T07-T26 | unit tests + assembleDebug; camera behind interface |
