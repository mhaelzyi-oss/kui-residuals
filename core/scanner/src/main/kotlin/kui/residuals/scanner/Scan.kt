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
    if (!directory) return FolderMark.UNKNOWN to "Not a folder."
    if (!looksLikePackageName(name)) return FolderMark.UNKNOWN to "Not a package name."
    if (packages.isSystemOrProtected(name)) return FolderMark.SKIP to "System or protected."
    if (packages.isRunning(name)) return FolderMark.SKIP to "Currently running."
    if (packages.isInstalled(name)) return FolderMark.SKIP to "Package is installed."
    return FolderMark.RESIDUAL to "No installed package."
}
