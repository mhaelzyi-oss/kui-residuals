package kui.residuals.access

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PathGuardTest {
    private val volume = defaultPrimaryVolume()
    private val data = volume.androidRoot(RootKind.DATA)
    private val guard = PathGuard(listOf(volume), FakeFileOps())

    @Test
    fun admitsPackageFolderUnderData() {
        val path = guard.admit("$data/com.example.old")
        assertEquals(RootKind.DATA, path.root)
        assertEquals("$data/com.example.old", path.canonical)
    }

    @Test
    fun admitsObbAndMedia() {
        assertEquals(RootKind.OBB, guard.admit("${volume.androidRoot(RootKind.OBB)}/com.foo").root)
        assertEquals(RootKind.MEDIA, guard.admit("${volume.androidRoot(RootKind.MEDIA)}/com.foo").root)
        assertEquals(RootKind.SET_ASIDE, guard.admit("${volume.androidRoot(RootKind.SET_ASIDE)}/com.foo/data").root)
        assertEquals(RootKind.COPIES, guard.admit("${volume.androidRoot(RootKind.COPIES)}/com.foo/data").root)
    }

    @Test
    fun rejectsDotDotEscape() {
        val err =
            assertFailsWith<AccessDenied> {
                guard.admit("$data/com.foo/../../../etc/passwd")
            }
        assertTrue(err.message!!.contains("Outside allowed roots") || err.message!!.contains("traversal"))
    }

    @Test
    fun rejectsRelativeAndEmpty() {
        assertFailsWith<AccessDenied> { guard.admit("Android/data/com.foo") }
        assertFailsWith<AccessDenied> { guard.admit("/") }
        assertFailsWith<AccessDenied> { guard.admit("/storage/emulated/0/Android/data/..") }
    }

    @Test
    fun rejectsSdcardAndDataData() {
        assertFailsWith<AccessDenied> { guard.admit("/sdcard/Download") }
        assertFailsWith<AccessDenied> { guard.admit("/data/data/com.example") }
        assertFailsWith<AccessDenied> { guard.admit("/storage/emulated/0/DCIM") }
    }

    @Test
    fun rejectsSymlinkAndFollowedEscape() {
        val linked =
            FakeFileOps(
                links =
                    mapOf(
                        "$data/evil" to "/mnt/outside/secret",
                    ),
            )
        val g = PathGuard(listOf(volume), linked)
        assertFailsWith<AccessDenied> { g.admit("$data/evil") }
    }

    @Test
    fun workProfileVolumeIsSeparateRoot() {
        val work = Volume(id = "10", emulatedRoot = "/storage/emulated/10")
        val both = PathGuard(listOf(volume, work), FakeFileOps())
        val path = both.admit("/storage/emulated/10/Android/data/com.work.app")
        assertEquals("10", path.volume.id)
        assertFailsWith<AccessDenied> {
            PathGuard(listOf(volume), FakeFileOps()).admit("/storage/emulated/10/Android/data/com.work.app")
        }
    }
}
