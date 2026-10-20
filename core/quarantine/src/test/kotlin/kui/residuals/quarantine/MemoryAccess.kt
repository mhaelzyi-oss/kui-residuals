package kui.residuals.quarantine

import kui.residuals.access.AccessDenied
import kui.residuals.access.AccessKind
import kui.residuals.access.AccessProvider
import kui.residuals.access.Dirent
import kui.residuals.access.GuardedPath
import kui.residuals.access.NodeStat
import kui.residuals.access.RootKind

class MemoryAccess(
    initial: Map<String, List<Dirent>> = emptyMap(),
    override val kind: AccessKind = AccessKind.SHIZUKU,
    override val available: Boolean = true,
) : AccessProvider {
    private val kids = initial.mapValues { it.value.toMutableList() }.toMutableMap()
    private val files = mutableMapOf<String, ByteArray>()

    override fun list(path: GuardedPath): Sequence<Dirent> =
        kids[path.canonical]?.asSequence() ?: emptySequence()

    override fun stat(path: GuardedPath): NodeStat =
        NodeStat(directory = !files.containsKey(path.canonical), size = files[path.canonical]?.size?.toLong() ?: 0)

    override fun mkdir(path: GuardedPath) {
        kids.putIfAbsent(path.canonical, mutableListOf())
        val parent = path.canonical.substringBeforeLast('/')
        val name = path.canonical.substringAfterLast('/')
        val list = kids.getOrPut(parent) { mutableListOf() }
        if (list.none { it.name == name }) list += Dirent(name, directory = true)
    }

    override fun move(from: GuardedPath, to: GuardedPath) {
        shift(from, to, copy = false)
    }

    override fun copy(from: GuardedPath, to: GuardedPath) {
        shift(from, to, copy = true)
    }

    override fun deleteTree(path: GuardedPath) {
        if (path.root != RootKind.SET_ASIDE) throw AccessDenied("Delete is only for folders already set aside.")
        drop(path.canonical, keepSource = false)
        val parent = path.canonical.substringBeforeLast('/')
        val name = path.canonical.substringAfterLast('/')
        kids[parent]?.removeAll { it.name == name }
    }

    override fun read(path: GuardedPath): ByteArray =
        files[path.canonical] ?: throw AccessDenied("Could not read.")

    override fun write(path: GuardedPath, bytes: ByteArray) {
        files[path.canonical] = bytes
        val parent = path.canonical.substringBeforeLast('/')
        val name = path.canonical.substringAfterLast('/')
        val list = kids.getOrPut(parent) { mutableListOf() }
        if (list.none { it.name == name }) list += Dirent(name, directory = false)
    }

    private fun shift(from: GuardedPath, to: GuardedPath, copy: Boolean) {
        val payload = (kids[from.canonical] ?: emptyList()).toMutableList()
        if (!copy) kids.remove(from.canonical)
        kids[to.canonical] = payload.toMutableList()
        val fromParent = from.canonical.substringBeforeLast('/')
        val fromName = from.canonical.substringAfterLast('/')
        if (!copy) kids[fromParent]?.removeAll { it.name == fromName }
        mkdir(to)
        kids[to.canonical] = payload.toMutableList()
        val prefix = from.canonical
        files.keys.filter { it == prefix || it.startsWith("$prefix/") }.forEach { key ->
            val dest = to.canonical + key.removePrefix(prefix)
            files[dest] = files.getValue(key)
            if (!copy) files.remove(key)
        }
    }

    private fun drop(prefix: String, keepSource: Boolean) {
        kids.keys.filter { it == prefix || it.startsWith("$prefix/") }.forEach { kids.remove(it) }
        files.keys.filter { it == prefix || it.startsWith("$prefix/") }.forEach { files.remove(it) }
        if (keepSource) Unit
    }
}
