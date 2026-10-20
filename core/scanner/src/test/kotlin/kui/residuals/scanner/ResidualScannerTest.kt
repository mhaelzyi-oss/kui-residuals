package kui.residuals.scanner

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kui.residuals.access.Dirent
import kui.residuals.access.PathGuard
import kui.residuals.access.RootKind
import kui.residuals.access.defaultPrimaryVolume
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ResidualScannerTest {
    private val volume = defaultPrimaryVolume()
    private val data = volume.androidRoot(RootKind.DATA)
    private val obb = volume.androidRoot(RootKind.OBB)
    private val media = volume.androidRoot(RootKind.MEDIA)
    private val guard = PathGuard(listOf(volume), MemoryFileOps())
    private val dispatcher = UnconfinedTestDispatcher()

    private fun scanner(
        children: Map<String, Sequence<Dirent>>,
        packages: PackageIndex,
        available: Boolean = true,
    ) = ResidualScanner(
        volumes = listOf(volume),
        guard = guard,
        access = MapAccess(children, available),
        packages = packages,
        dispatcher = dispatcher,
    )

    @Test
    fun residualUnknownAndSkip() =
        runTest(dispatcher) {
            val access =
                mapOf(
                    data to
                        sequenceOf(
                            Dirent("com.example.old", directory = true),
                            Dirent(".com.example.old", directory = true),
                            Dirent("com.example.live", directory = true),
                            Dirent("com.android.systemui", directory = true),
                            Dirent("com.running.now", directory = true),
                            Dirent("Cache", directory = true),
                            Dirent("notes.txt", directory = false),
                        ),
                    obb to emptySequence(),
                    media to emptySequence(),
                )
            val packages =
                FakePackages(
                    installed = setOf("com.example.live"),
                    running = setOf("com.running.now"),
                    system = setOf("com.android.systemui"),
                )
            val hits = mutableListOf<ScanHit>()
            var done: ScanProgress? = null
            scanner(access, packages).scan().test {
                while (true) {
                    when (val event = awaitItem()) {
                        is ScanEvent.Hit -> hits += event.hit
                        is ScanEvent.Done -> {
                            done = event.progress
                            break
                        }
                        is ScanEvent.Failed -> error(event.message)
                        is ScanEvent.Progress -> Unit
                    }
                }
                awaitComplete()
            }
            val byName = hits.associate { it.path.canonical.substringAfterLast('/') to it.mark }
            assertEquals(FolderMark.RESIDUAL, byName["com.example.old"])
            assertEquals(FolderMark.RESIDUAL, byName[".com.example.old"])
            assertEquals(FolderMark.SKIP, byName["com.example.live"])
            assertEquals(FolderMark.SKIP, byName["com.android.systemui"])
            assertEquals(FolderMark.SKIP, byName["com.running.now"])
            assertEquals(FolderMark.UNKNOWN, byName["Cache"])
            assertEquals(FolderMark.UNKNOWN, byName["notes.txt"])
            assertEquals(2, done!!.residual)
            assertEquals(2, done.unknown)
            assertEquals(3, done.skipped)
            assertEquals(7, done.visited)
            assertTrue(hits.first { it.path.canonical.endsWith("/.com.example.old") }.hidden)
        }

    @Test
    fun neverMarksUnknownAsResidual() =
        runTest(dispatcher) {
            val access =
                mapOf(
                    data to sequenceOf(Dirent(".thumbnails", directory = true), Dirent("obb", directory = true)),
                    obb to emptySequence(),
                    media to emptySequence(),
                )
            val hits = mutableListOf<ScanHit>()
            scanner(access, FakePackages()).scan().test {
                while (true) {
                    when (val event = awaitItem()) {
                        is ScanEvent.Hit -> hits += event.hit
                        is ScanEvent.Done -> break
                        is ScanEvent.Failed -> error(event.message)
                        is ScanEvent.Progress -> Unit
                    }
                }
                awaitComplete()
            }
            assertTrue(hits.isNotEmpty())
            assertTrue(hits.none { it.mark == FolderMark.RESIDUAL })
        }

    @Test
    fun cancelStopsBeforeTheEnd() =
        runTest(dispatcher) {
            val many =
                mapOf(
                    data to
                        sequence {
                            repeat(10_000) { yield(Dirent("com.pad.a$it", directory = true)) }
                        },
                    obb to emptySequence(),
                    media to emptySequence(),
                )
            var seen = 0
            val job =
                launch {
                    scanner(many, FakePackages()).scan().collect {
                        seen += 1
                        if (seen >= 8) cancel()
                    }
                }
            job.join()
            assertTrue(seen in 8..64)
        }

    @Test
    fun hugeFolderStreams() =
        runTest(dispatcher) {
            val huge =
                sequence {
                    yield(Dirent("com.orphan.app", directory = true))
                    repeat(10_000) { yield(Dirent("file-$it.bin", directory = false)) }
                }
            val access = mapOf(data to huge, obb to emptySequence(), media to emptySequence())
            var done: ScanProgress? = null
            scanner(access, FakePackages()).scan().test {
                while (true) {
                    when (val event = awaitItem()) {
                        is ScanEvent.Done -> {
                            done = event.progress
                            break
                        }
                        is ScanEvent.Failed -> error(event.message)
                        else -> Unit
                    }
                }
                awaitComplete()
            }
            assertEquals(10_001, done!!.visited)
            assertEquals(1, done.residual)
            assertEquals(10_000, done.unknown)
        }

    @Test
    fun failsWhenProviderMissing() =
        runTest(dispatcher) {
            scanner(emptyMap(), FakePackages(), available = false).scan().test {
                val failed = awaitItem() as ScanEvent.Failed
                assertEquals("No access provider.", failed.message)
                awaitComplete()
            }
        }
}
