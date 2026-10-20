package kui.residuals.access

data class ShellResult(
    val code: Int,
    val stdout: String,
    val stderr: String = "",
)

fun interface ShellSession {
    fun exec(command: String): ShellResult
}

interface DocumentTree {
    fun list(canonical: String): List<Dirent>

    fun mkdir(canonical: String) {
        throw AccessDenied("This tree cannot change files.")
    }

    fun move(from: String, to: String) {
        throw AccessDenied("This tree cannot change files.")
    }

    fun copy(from: String, to: String) {
        throw AccessDenied("This tree cannot change files.")
    }

    fun removeTree(canonical: String) {
        throw AccessDenied("This tree cannot change files.")
    }

    fun read(canonical: String): ByteArray {
        throw AccessDenied("This tree cannot read files.")
    }

    fun write(canonical: String, bytes: ByteArray) {
        throw AccessDenied("This tree cannot change files.")
    }
}
