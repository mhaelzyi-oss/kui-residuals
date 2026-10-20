package kui.residuals.quarantine

import kui.residuals.access.AccessDenied
import kui.residuals.access.AccessKind
import kui.residuals.access.Dirent
import kui.residuals.access.FileOps
import kui.residuals.access.PathGuard
import kui.residuals.access.RootKind
import kui.residuals.access.defaultPrimaryVolume
import kui.residuals.scanner.FolderMark
import kui.residuals.scanner.ScanHit
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SetAsideTest {
    private val volume = defaultPrimaryVolume()
    private val data = volume.androidRoot(RootKind.DATA)
    private val guard = PathGuard(listOf(volume), IdentityOps())
    private val leftover =
        ScanHit(
            path = guard.admit("$data/com.example.old"),
            mark = FolderMark.RESIDUAL,
            packageName = "com.example.old",
            reason = "No installed package.",
        )
    private val hidden =
        ScanHit(
            path = guard.admit("$data/.com.example.old"),
            mark = FolderMark.RESIDUAL,
            packageName = "com.example.old",
            reason = "Hidden leftover. The app is gone.",
            hidden = true,
        )

    @Test
    fun movesLeftoverWithGrantedAccess() {
        val access =
            MemoryAccess(
                mapOf(data to listOf(Dirent("com.example.old", directory = true))),
                kind = AccessKind.SAF,
            )
        val box = SetAside(listOf(volume), guard, access)
        val dest = box.setAside(leftover)
        assertEquals(RootKind.SET_ASIDE, dest.root)
        assertTrue(dest.canonical.endsWith("/com.example.old/data"))
        assertTrue(access.list(guard.admit(data)).none { it.name == "com.example.old" })
        assertEquals(1, box.list().size)
    }

    @Test
    fun savesACopyWithoutRemovingTheLeftover() {
        val access =
            MemoryAccess(
                mapOf(data to listOf(Dirent("com.example.old", directory = true))),
                kind = AccessKind.SAF,
            )
        val box = SetAside(listOf(volume), guard, access)
        val copy = box.saveCopy(leftover)
        assertEquals(RootKind.COPIES, copy.root)
        assertTrue(access.list(guard.admit(data)).any { it.name == "com.example.old" })
    }

    @Test
    fun hiddenLeftoverMovesWithTheSameAppName() {
        val access =
            MemoryAccess(
                mapOf(data to listOf(Dirent(".com.example.old", directory = true))),
            )
        val box = SetAside(listOf(volume), guard, access)
        val dest = box.setAside(hidden)
        assertTrue(dest.canonical.endsWith("/com.example.old/data"))
        box.deleteForever(dest)
        assertTrue(box.list().isEmpty())
    }

    @Test
    fun editsALeftoverFile() {
        val access =
            MemoryAccess(
                mapOf(data to listOf(Dirent("com.example.old", directory = true))),
                kind = AccessKind.SAF,
            )
        val file = guard.admit("$data/com.example.old/notes.txt")
        val box = SetAside(listOf(volume), guard, access)
        box.writeFile(file, "hello".toByteArray())
        assertContentEquals("hello".toByteArray(), box.readFile(file))
    }

    @Test
    fun deleteOnlySetAside() {
        val access =
            MemoryAccess(
                mapOf(data to listOf(Dirent("com.example.old", directory = true))),
            )
        val box = SetAside(listOf(volume), guard, access)
        val dest = box.setAside(leftover)
        box.deleteForever(dest)
        assertTrue(box.list().isEmpty())
        assertFailsWith<AccessDenied> { box.deleteForever(leftover.path) }
    }
}

private class IdentityOps : FileOps {
    override fun canonicalize(path: String): String = PathGuard.collapse(path)

    override fun isSymbolicLink(path: String): Boolean = false
}
