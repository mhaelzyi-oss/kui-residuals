package kui.residuals.access

/**
 * Shizuku talks over a shell identity (ADB/Sui). The Android binder client
 * is a later :app adapter that implements [ShellSession] and [alive]/[granted].
 */
class ShizukuAccessProvider(
    private val session: ShellSession,
    private val alive: () -> Boolean,
    private val granted: () -> Boolean,
) : AccessProvider {
    override val kind: AccessKind = AccessKind.SHIZUKU
    override val available: Boolean get() = alive() && granted()

    override fun list(path: GuardedPath): Sequence<Dirent> {
        requireAvailable()
        val result = session.exec("ls -1ap -- '${path.canonical}'")
        if (result.code != 0) throw AccessDenied(result.stderr.ifBlank { "Shizuku list refused." })
        return parseLs(result.stdout).asSequence()
    }

    override fun stat(path: GuardedPath): NodeStat {
        requireAvailable()
        val result = session.exec("stat -c '%F %s' -- '${path.canonical}'")
        if (result.code != 0) throw AccessDenied(result.stderr.ifBlank { "Shizuku stat refused." })
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
        if (!available) throw AccessDenied("Shizuku is not available.")
    }
}

internal fun parseLs(stdout: String): List<Dirent> =
    stdout
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && it != "." && it != ".." && it != "./" && it != "../" }
        .map { line ->
            val directory = line.endsWith("/")
            Dirent(name = line.removeSuffix("/"), directory = directory)
        }.toList()

internal fun parseStat(stdout: String): NodeStat {
    val line = stdout.trim().lineSequence().firstOrNull().orEmpty()
    val parts = line.split(Regex("\\s+"), limit = 2)
    val kind = parts.getOrNull(0).orEmpty().lowercase()
    val size = parts.getOrNull(1)?.toLongOrNull() ?: 0L
    return NodeStat(directory = kind.contains("directory"), size = size)
}
