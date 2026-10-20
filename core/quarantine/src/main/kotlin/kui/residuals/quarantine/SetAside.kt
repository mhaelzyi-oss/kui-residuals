package kui.residuals.quarantine

import kui.residuals.access.AccessDenied
import kui.residuals.access.AccessProvider
import kui.residuals.access.GuardedPath
import kui.residuals.access.PathGuard
import kui.residuals.access.RootKind
import kui.residuals.access.Volume
import kui.residuals.access.folderName
import kui.residuals.access.join
import kui.residuals.scanner.FolderMark
import kui.residuals.scanner.ScanHit

class SetAside(
    private val volumes: List<Volume>,
    private val guard: PathGuard,
    private val access: AccessProvider,
) {
    fun dest(hit: ScanHit, kind: RootKind = RootKind.SET_ASIDE): GuardedPath {
        val pkg = hit.packageName ?: hit.path.canonical.substringAfterLast('/').trimStart('.')
        val bucket = hit.path.root.folderName
        val raw = join(hit.path.volume.androidRoot(kind), pkg, bucket)
        return guard.admit(raw)
    }

    fun setAside(hit: ScanHit): GuardedPath {
        if (hit.mark != FolderMark.RESIDUAL) {
            throw AccessDenied("Only leftovers can be set aside.")
        }
        requireChange()
        val from = guard.admit(hit.path.canonical)
        val to = dest(hit, RootKind.SET_ASIDE)
        access.mkdir(guard.admit(from.volume.androidRoot(RootKind.SET_ASIDE)))
        access.mkdir(guard.admit(parentOf(to.canonical)))
        access.move(from, to)
        return guard.admit(to.canonical)
    }

    fun saveCopy(hit: ScanHit): GuardedPath {
        if (hit.mark != FolderMark.RESIDUAL && hit.path.root != RootKind.SET_ASIDE) {
            throw AccessDenied("Only leftovers can be saved as a copy.")
        }
        requireChange()
        val from = guard.admit(hit.path.canonical)
        val to = dest(hit, RootKind.COPIES)
        access.mkdir(guard.admit(from.volume.androidRoot(RootKind.COPIES)))
        access.mkdir(guard.admit(parentOf(to.canonical)))
        access.copy(from, to)
        return guard.admit(to.canonical)
    }

    fun writeFile(path: GuardedPath, bytes: ByteArray) {
        requireChange()
        val live = guard.admit(path.canonical)
        if (live.root != RootKind.SET_ASIDE && live.root != RootKind.COPIES && live.root != RootKind.DATA && live.root != RootKind.OBB && live.root != RootKind.MEDIA) {
            throw AccessDenied("Outside leftover folders.")
        }
        access.write(live, bytes)
    }

    fun readFile(path: GuardedPath): ByteArray {
        val live = guard.admit(path.canonical)
        return access.read(live)
    }

    fun deleteForever(path: GuardedPath) {
        requireChange()
        val live = guard.admit(path.canonical)
        if (live.root != RootKind.SET_ASIDE) {
            throw AccessDenied("Delete is only for folders already set aside.")
        }
        access.deleteTree(live)
    }

    fun list(): List<GuardedPath> {
        val out = mutableListOf<GuardedPath>()
        for (volume in volumes) {
            val root = guard.admit(volume.androidRoot(RootKind.SET_ASIDE))
            for (pkg in access.list(root)) {
                if (!pkg.directory) continue
                val pkgPath = guard.admit(join(root.canonical, pkg.name))
                for (bucket in access.list(pkgPath)) {
                    if (!bucket.directory) continue
                    out += guard.admit(join(pkgPath.canonical, bucket.name))
                }
            }
        }
        return out
    }

    private fun requireChange() {
        if (!access.available) throw AccessDenied("No access provider.")
        if (!access.canChange) throw AccessDenied("Need access you granted on this phone.")
    }

    private fun parentOf(canonical: String): String = canonical.substringBeforeLast('/')
}
