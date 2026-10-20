package kui.residuals.scanner

private val PACKAGE = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

fun looksLikePackageName(name: String): Boolean = PACKAGE.matches(name)
