package kui.residuals.access

enum class RootKind {
    DATA,
    OBB,
    MEDIA,
    SET_ASIDE,
    COPIES,
}

data class Volume(
    val id: String,
    val emulatedRoot: String,
) {
    init {
        require(emulatedRoot.startsWith("/")) { "Volume root must be absolute." }
    }

    fun androidRoot(kind: RootKind): String =
        when (kind) {
            RootKind.SET_ASIDE -> join(emulatedRoot, "KUIResiduals", "SetAside")
            RootKind.COPIES -> join(emulatedRoot, "KUIResiduals", "Copies")
            else -> join(emulatedRoot, "Android", kind.folderName)
        }
}

val RootKind.folderName: String
    get() =
        when (this) {
            RootKind.DATA -> "data"
            RootKind.OBB -> "obb"
            RootKind.MEDIA -> "media"
            RootKind.SET_ASIDE -> "SetAside"
            RootKind.COPIES -> "Copies"
        }

fun posix(path: String): String = path.replace('\\', '/')

fun join(vararg parts: String): String =
    parts
        .flatMap { posix(it).split('/') }
        .filter { it.isNotEmpty() }
        .joinToString(separator = "/", prefix = "/")

fun defaultPrimaryVolume(): Volume = Volume(id = "0", emulatedRoot = "/storage/emulated/0")
