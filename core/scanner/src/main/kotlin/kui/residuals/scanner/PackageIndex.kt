package kui.residuals.scanner

interface PackageIndex {
    fun isInstalled(pkg: String): Boolean

    fun isRunning(pkg: String): Boolean

    fun isSystemOrProtected(pkg: String): Boolean
}
