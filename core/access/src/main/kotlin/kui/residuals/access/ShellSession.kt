package kui.residuals.access

data class ShellResult(
    val code: Int,
    val stdout: String,
    val stderr: String = "",
)

fun interface ShellSession {
    fun exec(command: String): ShellResult
}

fun interface DocumentTree {
    fun list(canonical: String): List<Dirent>
}
