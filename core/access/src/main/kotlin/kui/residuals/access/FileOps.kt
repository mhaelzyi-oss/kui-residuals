package kui.residuals.access

interface FileOps {
    fun canonicalize(path: String): String

    fun isSymbolicLink(path: String): Boolean
}
