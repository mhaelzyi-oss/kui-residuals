package kui.residuals.access

/**
 * Optional libsu backend. [enabled] is a settings flag and defaults to false.
 * Never prompt for root from this module.
 */
class RootAccessProvider(
    private val session: ShellSession,
    private val enabled: () -> Boolean,
    private val granted: () -> Boolean,
) : AccessProvider {
    override val kind: AccessKind = AccessKind.ROOT
    override val available: Boolean get() = enabled() && granted()

    override fun list(path: GuardedPath): Sequence<Dirent> {
        requireAvailable()
        val result = session.exec("ls -1ap -- '${path.canonical}'")
        if (result.code != 0) throw AccessDenied(result.stderr.ifBlank { "Root list refused." })
        return parseLs(result.stdout).asSequence()
    }

    override fun stat(path: GuardedPath): NodeStat {
        requireAvailable()
        val result = session.exec("stat -c '%F %s' -- '${path.canonical}'")
        if (result.code != 0) throw AccessDenied(result.stderr.ifBlank { "Root stat refused." })
        return parseStat(result.stdout)
    }

    override fun mkdir(path: GuardedPath) {
        requireAvailable()
        session.mkdirp(path)
    }

    override fun move(from: GuardedPath, to: GuardedPath) {
        requireAvailable()
        session.moveInto(from, to)
    }

    override fun copy(from: GuardedPath, to: GuardedPath) {
        requireAvailable()
        session.copyInto(from, to)
    }

    override fun deleteTree(path: GuardedPath) {
        requireAvailable()
        session.removeTree(path)
    }

    override fun read(path: GuardedPath): ByteArray {
        requireAvailable()
        return session.catFile(path)
    }

    override fun write(path: GuardedPath, bytes: ByteArray) {
        requireAvailable()
        session.putFile(path, bytes)
    }

    private fun requireAvailable() {
        if (!available) throw AccessDenied("Root is off or not granted.")
    }
}
