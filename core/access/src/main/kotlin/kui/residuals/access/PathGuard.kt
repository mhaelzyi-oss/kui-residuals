package kui.residuals.access

data class GuardedPath(
    val canonical: String,
    val root: RootKind,
    val volume: Volume,
)

class PathGuard(
    private val volumes: List<Volume>,
    private val ops: FileOps,
) {
    init {
        require(volumes.isNotEmpty()) { "At least one volume is required." }
    }

    fun admit(raw: String): GuardedPath {
        val collapsed = collapse(raw)
        val canonical = collapse(ops.canonicalize(collapsed))
        rejectSymlinks(canonical)
        val hit =
            match(canonical)
                ?: throw AccessDenied("Outside allowed roots.")
        return GuardedPath(canonical = canonical, root = hit.first, volume = hit.second)
    }

    private fun rejectSymlinks(canonical: String) {
        for (prefix in prefixes(canonical)) {
            if (ops.isSymbolicLink(prefix)) {
                throw AccessDenied("Symlink rejected.")
            }
        }
    }

    private fun match(canonical: String): Pair<RootKind, Volume>? {
        for (volume in volumes) {
            for (kind in RootKind.entries) {
                val root = volume.androidRoot(kind)
                if (canonical == root || canonical.startsWith("$root/")) {
                    return kind to volume
                }
            }
        }
        return null
    }

    companion object {
        fun collapse(raw: String): String {
            val unix = posix(raw.trim())
            if (unix.isEmpty() || unix == "/") {
                throw AccessDenied("Path traversal.")
            }
            if (!unix.startsWith("/")) {
                throw AccessDenied("Path must be absolute.")
            }
            val parts = ArrayDeque<String>()
            for (seg in unix.split('/')) {
                when {
                    seg.isEmpty() || seg == "." -> Unit
                    seg == ".." -> {
                        if (parts.isEmpty()) throw AccessDenied("Path traversal.")
                        parts.removeLast()
                    }
                    seg.contains('\u0000') -> throw AccessDenied("Path traversal.")
                    else -> parts.addLast(seg)
                }
            }
            if (parts.isEmpty()) throw AccessDenied("Path traversal.")
            return parts.joinToString(separator = "/", prefix = "/")
        }

        fun prefixes(canonical: String): List<String> {
            val parts = canonical.split('/').filter { it.isNotEmpty() }
            return parts.indices.map { i ->
                parts.take(i + 1).joinToString(separator = "/", prefix = "/")
            }
        }
    }
}
