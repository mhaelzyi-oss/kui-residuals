package kui.residuals.access

enum class AccessKind {
    SHIZUKU,
    ROOT,
    SAF,
}

enum class AccessState {
    SHIZUKU,
    ROOT,
    SAF,
    NONE,
    LOST,
}

data class Dirent(
    val name: String,
    val directory: Boolean,
) {
    val hidden: Boolean get() = name.startsWith(".")
}

data class NodeStat(
    val directory: Boolean,
    val size: Long,
    val countHint: Long? = null,
)

interface AccessProvider {
    val kind: AccessKind
    val available: Boolean

    val canChange: Boolean
        get() = available

    fun list(path: GuardedPath): Sequence<Dirent>

    fun stat(path: GuardedPath): NodeStat

    fun mkdir(path: GuardedPath) {
        throw AccessDenied("This access cannot change files.")
    }

    fun move(from: GuardedPath, to: GuardedPath) {
        throw AccessDenied("This access cannot change files.")
    }

    fun copy(from: GuardedPath, to: GuardedPath) {
        throw AccessDenied("This access cannot change files.")
    }

    fun deleteTree(path: GuardedPath) {
        throw AccessDenied("This access cannot change files.")
    }

    fun read(path: GuardedPath): ByteArray {
        throw AccessDenied("This access cannot read files.")
    }

    fun write(path: GuardedPath, bytes: ByteArray) {
        throw AccessDenied("This access cannot change files.")
    }
}

class AccessBroker(
    private val shizuku: AccessProvider,
    private val root: AccessProvider,
    private val saf: AccessProvider,
    private val rootEnabled: () -> Boolean = { false },
) {
    var state: AccessState = AccessState.NONE
        private set

    fun pick(): AccessProvider? {
        if (state == AccessState.LOST) return null
        val chosen =
            when {
                shizuku.available -> shizuku
                saf.available -> saf
                rootEnabled() && root.available -> root
                else -> null
            }
        state =
            when (chosen?.kind) {
                AccessKind.SHIZUKU -> AccessState.SHIZUKU
                AccessKind.ROOT -> AccessState.ROOT
                AccessKind.SAF -> AccessState.SAF
                null -> AccessState.NONE
            }
        return chosen
    }

    fun onBinderDied() {
        state = AccessState.LOST
    }

    fun retryAfterSetup() {
        if (state == AccessState.LOST) state = AccessState.NONE
        pick()
    }
}
