package kui.residuals.scanner

import java.io.File
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kui.residuals.access.Dirent
import kui.residuals.access.PathGuard
import kui.residuals.access.RootKind
import kui.residuals.access.defaultPrimaryVolume

fun main() {
    val volume = defaultPrimaryVolume()
    val data = volume.androidRoot(RootKind.DATA)
    val obb = volume.androidRoot(RootKind.OBB)
    val media = volume.androidRoot(RootKind.MEDIA)
    val guard = PathGuard(listOf(volume), MemoryFileOps())
    val access =
        MapAccess(
            mapOf(
                data to
                    sequenceOf(
                        Dirent("com.example.old", directory = true),
                        Dirent(".com.example.old", directory = true),
                        Dirent("com.example.live", directory = true),
                        Dirent("com.android.systemui", directory = true),
                        Dirent("com.running.now", directory = true),
                        Dirent("Cache", directory = true),
                        Dirent(".thumbnails", directory = true),
                        Dirent("notes.txt", directory = false),
                    ),
                obb to sequenceOf(Dirent("com.example.old", directory = true)),
                media to emptySequence(),
            ),
        )
    val packages =
        FakePackages(
            installed = setOf("com.example.live"),
            running = setOf("com.running.now"),
            system = setOf("com.android.systemui"),
        )
    val scanner = ResidualScanner(listOf(volume), guard, access, packages)
    val events = runBlocking { scanner.scan().toList() }
    val hits = events.filterIsInstance<ScanEvent.Hit>().map { it.hit }
    val template = File("core/scanner/src/test/resources/app-preview.html").readText(Charsets.UTF_8)
    val dest = File("core/scanner/build/demo-scan.html")
    dest.parentFile.mkdirs()
    dest.writeText(template.replace("__SCAN__", json(hits)), Charsets.UTF_8)
    println(dest.absolutePath)
}

private fun json(hits: List<ScanHit>): String =
    hits.joinToString(prefix = "{\"hits\":[", postfix = "]}") { hit ->
        val mark =
            when (hit.mark) {
                FolderMark.RESIDUAL -> "leftover"
                FolderMark.SKIP -> "still"
                FolderMark.UNKNOWN -> "other"
            }
        val why =
            when (hit.mark) {
                FolderMark.RESIDUAL -> if (hit.hidden) "hidden" else "gone"
                FolderMark.SKIP ->
                    when {
                        hit.reason.contains("running", ignoreCase = true) -> "running"
                        hit.reason.contains("system", ignoreCase = true) -> "system"
                        else -> "installed"
                    }
                FolderMark.UNKNOWN ->
                    if (hit.reason.contains("Not a folder", ignoreCase = true)) "file" else "name"
            }
        val name = hit.path.canonical.substringAfterLast('/')
        val pkg = hit.packageName ?: name
        val path = hit.path.canonical.removePrefix("/storage/emulated/0/").ifBlank { hit.path.canonical }
        """{"name":"${esc(pkg)}","folder":"${esc(name)}","label":"${esc(labelOf(name))}","mark":"$mark","path":"${esc(path)}","why":"$why","hidden":${hit.hidden}}"""
    }

private fun labelOf(name: String): String =
    when (name) {
        "com.example.old", ".com.example.old" -> "Old Example"
        "com.example.live" -> "Live Example"
        "com.android.systemui" -> "Android System"
        "com.running.now" -> "Running Now"
        ".thumbnails" -> "Thumbnails"
        else -> name
    }

private fun esc(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
