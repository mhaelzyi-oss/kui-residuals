package kui.residuals.scanner

import kui.residuals.access.GuardedPath

enum class FolderMark {
    RESIDUAL,
    SKIP,
    UNKNOWN,
}

data class ScanHit(
    val path: GuardedPath,
    val mark: FolderMark,
    val packageName: String?,
    val reason: String,
    val hidden: Boolean = false,
)

data class ScanProgress(
    val visited: Long,
    val residual: Long,
    val unknown: Long,
    val skipped: Long,
    val current: String?,
)

sealed class ScanEvent {
    data class Progress(val progress: ScanProgress) : ScanEvent()

    data class Hit(val hit: ScanHit) : ScanEvent()

    data class Done(val progress: ScanProgress) : ScanEvent()

    data class Failed(val message: String) : ScanEvent()
}

internal fun classify(
    name: String,
    directory: Boolean,
    packages: PackageIndex,
): Pair<FolderMark, String> {
    val pkg = packageNameOf(name)
    val hidden = name.startsWith(".")
    if (!directory) {
        if (!hidden || pkg == null) return FolderMark.UNKNOWN to "Not a folder."
        return classifyPackage(pkg, packages, hidden)
    }
    if (pkg == null) return FolderMark.UNKNOWN to "Not a package name."
    return classifyPackage(pkg, packages, hidden)
}

private fun classifyPackage(
    pkg: String,
    packages: PackageIndex,
    hidden: Boolean,
): Pair<FolderMark, String> {
    if (packages.isSystemOrProtected(pkg)) return FolderMark.SKIP to "System or protected."
    if (packages.isRunning(pkg)) return FolderMark.SKIP to "Currently running."
    if (packages.isInstalled(pkg)) return FolderMark.SKIP to "Package is installed."
    val why = if (hidden) "Hidden leftover. The app is gone." else "No installed package."
    return FolderMark.RESIDUAL to why
}
