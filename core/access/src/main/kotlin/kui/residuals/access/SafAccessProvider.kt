package kui.residuals.access

/**
 * SAF fallback. [treeSelected] is true only after the user picked a tree.
 * Callers must still pass a [GuardedPath] so SAF cannot wander.
 */
class SafAccessProvider(
    private val tree: DocumentTree,
    private val treeSelected: () -> Boolean,
) : AccessProvider {
    override val kind: AccessKind = AccessKind.SAF
    override val available: Boolean get() = treeSelected()

    override fun list(path: GuardedPath): Sequence<Dirent> {
        if (!available) throw AccessDenied("No SAF tree selected.")
        return tree.list(path.canonical).asSequence()
    }

    override fun stat(path: GuardedPath): NodeStat {
        if (!available) throw AccessDenied("No SAF tree selected.")
        val kids = tree.list(path.canonical)
        return NodeStat(directory = true, size = 0, countHint = kids.size.toLong())
    }
}
