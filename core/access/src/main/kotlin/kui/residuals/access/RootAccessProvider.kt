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
        val result = session.exec("ls -1p -- '${path.canonical}'")
        if (result.code != 0) throw AccessDenied(result.stderr.ifBlank { "Root list refused." })
        return parseLs(result.stdout).asSequence()
    }

    override fun stat(path: GuardedPath): NodeStat {
        requireAvailable()
        val result = session.exec("stat -c '%F %s' -- '${path.canonical}'")
        if (result.code != 0) throw AccessDenied(result.stderr.ifBlank { "Root stat refused." })
        return parseStat(result.stdout)
    }

    private fun requireAvailable() {
        if (!available) throw AccessDenied("Root is off or not granted.")
    }
}
