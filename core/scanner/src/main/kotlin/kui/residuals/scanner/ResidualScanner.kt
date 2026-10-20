package kui.residuals.scanner

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.yield
import kui.residuals.access.AccessDenied
import kui.residuals.access.AccessProvider
import kui.residuals.access.PathGuard
import kui.residuals.access.RootKind
import kui.residuals.access.Volume
import kui.residuals.access.join

class ResidualScanner(
    private val volumes: List<Volume>,
    private val guard: PathGuard,
    private val access: AccessProvider,
    private val packages: PackageIndex,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    fun scan(): Flow<ScanEvent> =
        flow {
            if (!access.available) {
                emit(ScanEvent.Failed("No access provider."))
                return@flow
            }
            var visited = 0L
            var residual = 0L
            var unknown = 0L
            var skipped = 0L
            try {
                for (volume in volumes) {
                    for (kind in RootKind.entries) {
                        currentCoroutineContext().ensureActive()
                        val root = guard.admit(volume.androidRoot(kind))
                        for (entry in access.list(root)) {
                            currentCoroutineContext().ensureActive()
                            yield()
                            visited += 1
                            val (mark, reason) = classify(entry.name, entry.directory, packages)
                            when (mark) {
                                FolderMark.RESIDUAL -> residual += 1
                                FolderMark.UNKNOWN -> unknown += 1
                                FolderMark.SKIP -> skipped += 1
                            }
                            val child =
                                runCatching { guard.admit(join(root.canonical, entry.name)) }.getOrNull()
                            val progress =
                                ScanProgress(
                                    visited = visited,
                                    residual = residual,
                                    unknown = unknown,
                                    skipped = skipped,
                                    current = entry.name,
                                )
                            if (child != null) {
                                emit(
                                    ScanEvent.Hit(
                                        ScanHit(
                                            path = child,
                                            mark = mark,
                                            packageName = entry.name.takeIf { looksLikePackageName(it) },
                                            reason = reason,
                                        ),
                                    ),
                                )
                            }
                            if (visited % 64L == 0L) emit(ScanEvent.Progress(progress))
                        }
                    }
                }
                emit(
                    ScanEvent.Done(
                        ScanProgress(
                            visited = visited,
                            residual = residual,
                            unknown = unknown,
                            skipped = skipped,
                            current = null,
                        ),
                    ),
                )
            } catch (denied: AccessDenied) {
                emit(ScanEvent.Failed(denied.message ?: "Access denied."))
            }
        }.flowOn(dispatcher)
}
