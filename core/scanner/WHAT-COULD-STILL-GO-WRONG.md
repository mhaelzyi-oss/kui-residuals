# What could still go wrong (`:core:scanner`)

- Only first-level children of the three roots are classified. A leftover nested under a still-installed package is invisible here; that is a later product choice, not a miss in this module.
- `PackageIndex` is a snapshot the `:app` must fill from all users and work profiles. If the adapter omits a user, a live app’s folder becomes a residual.
- `isRunning` depends on whoever implements the index. A frozen or cached process list will skip too little or too much.
- `looksLikePackageName` rejects single-segment names. OEM folders that are real packages with unusual names stay `UNKNOWN` (safe) rather than residual.
- Mid-scan install: classify calls `isInstalled` per folder, but a package can appear after that call and before quarantine. Recheck immediately before any move remains mandatory in `:core:data`.
- `AccessDenied` becomes `Failed` and stops the whole scan. A single unreadable root should perhaps continue the others; that is not implemented.
- `flowOn` plus a real Shizuku `list()` that blocks in native code will not see `cancel()` until the syscall returns.
- Huge `list()` sequences are streamed, but each `Hit` still allocates a `GuardedPath`. UI collectors that keep every hit will blow memory; the scanner does not.
