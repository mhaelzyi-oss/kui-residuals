package kui.residuals.access

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MutateTest {
    private val volume = defaultPrimaryVolume()
    private val guard = PathGuard(listOf(volume), FakeFileOps())

    @Test
    fun shizukuDeletesOnlySetAside() {
        val data = guard.admit("${volume.androidRoot(RootKind.DATA)}/com.example.old")
        val held = guard.admit("${volume.androidRoot(RootKind.SET_ASIDE)}/com.example.old/data")
        val shizuku =
            ShizukuAccessProvider(
                session =
                    ScriptedShell(
                        mapOf(
                            "rm -rf -- '${held.canonical}'" to ShellResult(0, ""),
                        ),
                    ),
                alive = { true },
                granted = { true },
            )
        assertFailsWith<AccessDenied> { shizuku.deleteTree(data) }
        shizuku.deleteTree(held)
        assertTrue(shizuku.canChange)
    }

    @Test
    fun foldersYouPickedCanChangeWithoutRoot() {
        val held = guard.admit("${volume.androidRoot(RootKind.SET_ASIDE)}/com.example.old/data")
        val tree = MutableDocumentTree()
        tree.mkdir(held.canonical)
        val saf = SafAccessProvider(tree, treeSelected = { true })
        assertTrue(saf.canChange)
        saf.deleteTree(held)
        assertTrue(tree.list(held.canonical.substringBeforeLast('/')).none { it.name == "data" })
    }

    @Test
    fun writesASmallFile() {
        val file = guard.admit("${volume.androidRoot(RootKind.DATA)}/com.example.old/notes.txt")
        val tree = MutableDocumentTree()
        val saf = SafAccessProvider(tree, treeSelected = { true })
        saf.write(file, "keep".toByteArray())
        assertContentEquals("keep".toByteArray(), saf.read(file))
    }
}

class MutableDocumentTree : DocumentTree {
    private val kids = mutableMapOf<String, MutableList<Dirent>>()
    private val files = mutableMapOf<String, ByteArray>()

    override fun list(canonical: String): List<Dirent> = kids[canonical].orEmpty()

    override fun mkdir(canonical: String) {
        kids.putIfAbsent(canonical, mutableListOf())
        attach(canonical, directory = true)
    }

    override fun removeTree(canonical: String) {
        kids.keys.filter { it == canonical || it.startsWith("$canonical/") }.forEach { kids.remove(it) }
        files.keys.filter { it == canonical || it.startsWith("$canonical/") }.forEach { files.remove(it) }
        val parent = canonical.substringBeforeLast('/')
        val name = canonical.substringAfterLast('/')
        kids[parent]?.removeAll { it.name == name }
    }

    override fun read(canonical: String): ByteArray = files[canonical] ?: throw AccessDenied("Could not read.")

    override fun write(canonical: String, bytes: ByteArray) {
        files[canonical] = bytes
        attach(canonical, directory = false)
    }

    private fun attach(canonical: String, directory: Boolean) {
        val parent = canonical.substringBeforeLast('/')
        val name = canonical.substringAfterLast('/')
        val list = kids.getOrPut(parent) { mutableListOf() }
        if (list.none { it.name == name }) list += Dirent(name, directory)
    }
}
