# KUI Residuals

See leftover folders from apps you already removed, including hidden names that linger. Open files. Edit them. Set them aside. Save a copy. Delete forever only after that, with a caution confirm.

If a name is not an app, we leave it. Nothing is uploaded. Root is not required — pick the folders, or use Shizuku if you already have it.

Phone app: Leftovers, Files, Debug. Desktop twin is **KUI Desk** (`kui-prompt`). That computer talks to several phones you plug in and can do the same leftover work on each one.

```text
./gradlew :core:access:test :core:scanner:test :core:quarantine:test
./gradlew :core:scanner:demoScan
./gradlew :app:assembleDebug
```

Install the debug APK from `app/build/outputs/apk/debug/` after an Android SDK is present. Read TERMS.md.
