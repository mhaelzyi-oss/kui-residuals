package kui.residuals.access

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class ToggleProvider(
    override val kind: AccessKind,
    var on: Boolean,
    private val children: List<Dirent> = emptyList(),
) : AccessProvider {
    override val available: Boolean get() = on

    override fun list(path: GuardedPath): Sequence<Dirent> = children.asSequence()

    override fun stat(path: GuardedPath): NodeStat = NodeStat(directory = true, size = 0)
}

class AccessBrokerTest {
    private val volume = defaultPrimaryVolume()
    private val guard = PathGuard(listOf(volume), FakeFileOps())
    private val data = guard.admit(volume.androidRoot(RootKind.DATA))

    @Test
    fun prefersShizukuThenFoldersYouPickedThenOptionalRoot() {
        val shizuku = ToggleProvider(AccessKind.SHIZUKU, on = true)
        val root = ToggleProvider(AccessKind.ROOT, on = true)
        val saf = ToggleProvider(AccessKind.SAF, on = true)
        val broker = AccessBroker(shizuku, root, saf, rootEnabled = { true })
        assertEquals(AccessKind.SHIZUKU, broker.pick()?.kind)

        shizuku.on = false
        assertEquals(AccessKind.SAF, broker.pick()?.kind)

        saf.on = false
        assertEquals(AccessKind.ROOT, broker.pick()?.kind)
    }

    @Test
    fun noneWhenNothingAvailable() {
        val broker =
            AccessBroker(
                ToggleProvider(AccessKind.SHIZUKU, on = false),
                ToggleProvider(AccessKind.ROOT, on = false),
                ToggleProvider(AccessKind.SAF, on = false),
            )
        assertNull(broker.pick())
        assertEquals(AccessState.NONE, broker.state)
    }

    @Test
    fun binderDeathStopsFallback() {
        val shizuku = ToggleProvider(AccessKind.SHIZUKU, on = true)
        val saf = ToggleProvider(AccessKind.SAF, on = true)
        val broker =
            AccessBroker(
                shizuku,
                ToggleProvider(AccessKind.ROOT, on = false),
                saf,
            )
        assertEquals(AccessKind.SHIZUKU, broker.pick()?.kind)
        broker.onBinderDied()
        shizuku.on = false
        assertNull(broker.pick())
        assertEquals(AccessState.LOST, broker.state)
        broker.retryAfterSetup()
        assertEquals(AccessKind.SAF, broker.pick()?.kind)
    }

    @Test
    fun shizukuListsThroughGuard() {
        val session =
            ScriptedShell(
                mapOf(
                    "ls -1ap -- '${data.canonical}'" to
                        ShellResult(0, "com.example.old/\n.com.example.old/\nCache\nunknown.bin\n"),
                    "stat -c '%F %s' -- '${data.canonical}'" to
                        ShellResult(0, "directory 4096\n"),
                ),
            )
        val provider = ShizukuAccessProvider(session, alive = { true }, granted = { true })
        val names = provider.list(data).map { it.name }.toList()
        assertEquals(listOf("com.example.old", ".com.example.old", "Cache", "unknown.bin"), names)
        assertTrue(provider.list(data).first().directory)
        assertTrue(provider.list(data).toList()[1].hidden)
        assertEquals(4096, provider.stat(data).size)
    }

    @Test
    fun safCannotSkipGuard() {
        val tree =
            MapDocumentTree(
                mapOf(
                    data.canonical to listOf(Dirent("com.foo", directory = true)),
                ),
            )
        val saf = SafAccessProvider(tree, treeSelected = { true })
        assertEquals("com.foo", saf.list(data).first().name)
        assertFailsWith<AccessDenied> {
            PathGuard(listOf(volume), FakeFileOps()).admit("/sdcard/Download")
        }
    }

    @Test
    fun rootStaysOffUntilSettingsAndGrant() {
        val root =
            RootAccessProvider(
                session = ScriptedShell(emptyMap()),
                enabled = { false },
                granted = { true },
            )
        assertEquals(false, root.available)
        assertFailsWith<AccessDenied> { root.list(data) }
    }
}
