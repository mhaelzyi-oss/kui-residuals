package kui.residuals.scanner

import kui.residuals.access.AccessKind
import kui.residuals.access.FileOps
import kui.residuals.access.PathGuard
import kui.residuals.access.AccessProvider
import kui.residuals.access.Dirent
import kui.residuals.access.GuardedPath
import kui.residuals.access.NodeStat

class FakePackages(
    private val installed: Set<String> = emptySet(),
    private val running: Set<String> = emptySet(),
    private val system: Set<String> = emptySet(),
) : PackageIndex {
    override fun isInstalled(pkg: String): Boolean = pkg in installed

    override fun isRunning(pkg: String): Boolean = pkg in running

    override fun isSystemOrProtected(pkg: String): Boolean = pkg in system
}

class MemoryFileOps(
    private val links: Map<String, String> = emptyMap(),
) : FileOps {
    override fun canonicalize(path: String): String {
        val collapsed = PathGuard.collapse(path)
        return links[collapsed]?.let { PathGuard.collapse(it) } ?: collapsed
    }

    override fun isSymbolicLink(path: String): Boolean = links.containsKey(PathGuard.collapse(path))
}

class MapAccess(
    private val children: Map<String, Sequence<Dirent>>,
    override val available: Boolean = true,
) : AccessProvider {
    override val kind: AccessKind = AccessKind.SHIZUKU

    override fun list(path: GuardedPath): Sequence<Dirent> = children[path.canonical] ?: emptySequence()

    override fun stat(path: GuardedPath): NodeStat = NodeStat(directory = true, size = 0)
}
