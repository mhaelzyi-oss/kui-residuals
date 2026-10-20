# What could still go wrong (`:core:access`)

- `canonicalize` on a real device can differ from `collapse()` if the OEM bind-mounts `/storage/emulated/0`. Need a device FileOps that uses `realpath`/`Os.readlink` before admit.
- `ls -1ap` is not toybox-stable on every image. Some shells ignore `-p`; directories then look like files until scanner stats them. Hidden names need `-a`.
- Shizuku binder death is only a callback into `AccessBroker.onBinderDied()`. If the adapter forgets to register `Shizuku.addBinderReceivedListener`, scans keep running with a dead session.
- Work profile volume `/storage/emulated/10` is modeled, but discovering volumes still belongs to `:app`. A missing volume list will hide residuals on the other user.
- SAF can return a tree that is not actually `Android/data`. Guard still rejects paths; the UI must not treat a successful `pick()` as a full scan.
- Root `available` is settings ∧ grant. A future UI that “helpfully” sets the flag true on first launch bypasses the product rule.
- Delete is `rm -rf` only after PathGuard admits a SET_ASIDE path. Do not call it from the scanner.
- Symlink checks use each prefix. A link replaced after admit (TOCTOU) is a scanner/quarantine problem: re-admit immediately before any move.
- Quote wrapping in `ls -- 'path'` is not a full shell-escape. Guarded paths are numeric/package-like today; do not pass raw user strings to `exec`.
