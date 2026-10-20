package kui.residuals.access

/**
 * Folders the person picked. Enough to look, edit, set aside, and delete
 * with caution. Root is never required.
 */
class SafAccessProvider(
    private val tree: DocumentTree,
    private val treeSelected: () -> Boolean,
) : AccessProvider {
    override val kind: AccessKind = AccessKind.SAF
    override val available: Boolean get() = treeSelected()

    override fun list(path: GuardedPath): Sequence<Dirent> {
        requireAvailable()
        return tree.list(path.canonical).asSequence()
    }

    override fun stat(path: GuardedPath): NodeStat {
        requireAvailable()
        val kids = tree.list(path.canonical)
        return NodeStat(directory = true, size = 0, countHint = kids.size.toLong())
    }

    override fun mkdir(path: GuardedPath) {
        requireAvailable()
        tree.mkdir(path.canonical)
    }

    override fun move(from: GuardedPath, to: GuardedPath) {
        requireAvailable()
        tree.move(from.canonical, to.canonical)
    }

    override fun copy(from: GuardedPath, to: GuardedPath) {
        requireAvailable()
        tree.copy(from.canonical, to.canonical)
    }

    override fun deleteTree(path: GuardedPath) {
        requireAvailable()
        if (path.root != RootKind.SET_ASIDE) {
            throw AccessDenied("Delete is only for folders already set aside.")
        }
        tree.removeTree(path.canonical)
    }

    override fun read(path: GuardedPath): ByteArray {
        requireAvailable()
        return tree.read(path.canonical)
    }

    override fun write(path: GuardedPath, bytes: ByteArray) {
        requireAvailable()
        tree.write(path.canonical, bytes)
    }

    private fun requireAvailable() {
        if (!available) throw AccessDenied("Pick the folders on this phone first.")
    }
}
