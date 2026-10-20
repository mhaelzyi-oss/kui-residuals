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
    val dest = File("core/scanner/build/demo-scan.html")
    dest.parentFile.mkdirs()
    dest.writeText(renderHtml(hits), Charsets.UTF_8)
    println(dest.absolutePath)
}

private fun renderHtml(hits: List<ScanHit>): String {
    val leftovers = hits.filter { it.mark == FolderMark.RESIDUAL }
    val needed = hits.filter { it.mark == FolderMark.SKIP }
    val ignored = hits.filter { it.mark == FolderMark.UNKNOWN }
    val leftoverApps = leftovers.groupBy { it.packageName ?: nameOf(it) }
    val leftoverPlaces = leftovers.size
    val leftoverLead =
        when (leftoverApps.size) {
            0 -> "No leftover app folders on this scan."
            1 ->
                "One uninstalled app left ${count(leftoverPlaces, "folder")} behind. " +
                    "We have not moved or deleted anything."
            else ->
                "${leftoverApps.size} uninstalled apps left ${count(leftoverPlaces, "folder")} behind. " +
                    "We have not moved or deleted anything."
        }

    val leftoverCards =
        leftoverApps.entries.joinToString("") { (pkg, places) ->
            val where =
                places.joinToString("") { hit ->
                    """<li><strong>${esc(placeTitle(hit.path.canonical))}</strong><span>${esc(shortPath(hit.path.canonical))}</span></li>"""
                }
            """
            <article class="card leftover">
              <p class="kicker">Leftover from an uninstalled app</p>
              <h3>${esc(pkg)}</h3>
              <p>This app is not on the phone anymore. Android left its files here.</p>
              <ul>$where</ul>
            </article>
            """.trimIndent()
        }

    val neededCards =
        needed.joinToString("") { hit ->
            """
            <article class="card needed">
              <p class="kicker">${esc(neededKicker(hit))}</p>
              <h3>${esc(nameOf(hit))}</h3>
              <p>${esc(neededWhy(hit))}</p>
              <p class="path">${esc(shortPath(hit.path.canonical))}</p>
            </article>
            """.trimIndent()
        }

    val ignoredCards =
        ignored.joinToString("") { hit ->
            """
            <article class="card ignored">
              <p class="kicker">Not an app leftover</p>
              <h3>${esc(nameOf(hit))}</h3>
              <p>${esc(ignoredWhy(hit))}</p>
              <p class="path">${esc(shortPath(hit.path.canonical))}</p>
            </article>
            """.trimIndent()
        }

    return """
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <title>KUI Residuals</title>
  <link rel="preconnect" href="https://fonts.googleapis.com"/>
  <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Instrument+Sans:wght@400;500;600;700&display=swap"/>
  <style>
    :root {
      --cream: #f7f4ee;
      --gold: #c9a227;
      --gold-line: #e4cf7a;
      --ink: #0a0a0a;
      --mute: #5f6368;
      --label: #8c6d12;
      --card: rgb(249 250 251 / 86%);
      --line: rgb(0 0 0 / 8%);
    }
    * { box-sizing: border-box; }
    html, body { margin: 0; background: var(--cream); color: var(--ink); font-family: "Instrument Sans", "Segoe UI", sans-serif; }
    body { min-height: 100vh; }
    .phone { max-width: 28rem; margin: 0 auto; padding: 0 1rem 2.5rem; }
    header { display: flex; justify-content: space-between; align-items: baseline; padding: 1rem 0; border-bottom: 1px solid var(--gold-line); color: var(--label); font-size: 0.82rem; font-weight: 600; }
    h1 { font-size: 1.65rem; letter-spacing: -0.03em; line-height: 1.15; margin: 1.25rem 0 0.5rem; }
    .intro { color: var(--mute); margin: 0 0 1.25rem; line-height: 1.45; }
    .counts { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 0.6rem; margin-bottom: 1.5rem; }
    .count { background: var(--card); border: 1px solid var(--line); border-radius: 14px; padding: 0.85rem 0.7rem; }
    .count b { display: block; font-size: 1.35rem; color: var(--gold); }
    .count span { display: block; font-size: 0.8rem; line-height: 1.25; color: var(--label); margin-top: 0.2rem; }
    section { margin: 0 0 1.6rem; }
    h2 { font-size: 0.95rem; margin: 0 0 0.35rem; }
    .lead { color: var(--mute); font-size: 0.9rem; margin: 0 0 0.85rem; line-height: 1.4; }
    .card { background: var(--card); border: 1px solid var(--line); border-radius: 16px; padding: 0.95rem 1rem; margin: 0 0 0.7rem; }
    .leftover { border-color: rgb(201 162 39 / 45%); background: rgb(243 230 184 / 42%); }
    .ignored { border-style: dashed; }
    .kicker { margin: 0 0 0.25rem; font-size: 0.68rem; letter-spacing: 0.08em; text-transform: uppercase; font-weight: 700; color: var(--label); }
    h3 { margin: 0 0 0.35rem; font-size: 1.02rem; word-break: break-word; }
    .card p { margin: 0; font-size: 0.9rem; line-height: 1.4; }
    .path, .card li span { display: block; color: var(--mute); font-size: 0.78rem; margin-top: 0.35rem; word-break: break-all; }
    ul { list-style: none; margin: 0.7rem 0 0; padding: 0; }
    li { padding: 0.45rem 0 0; border-top: 1px solid rgb(201 162 39 / 25%); }
    li:first-child { border-top: 0; padding-top: 0; }
    button { width: 100%; border: 0; border-radius: 12px; padding: 0.9rem; background: var(--gold); color: #1a1404; font: inherit; font-weight: 700; opacity: 0.45; cursor: not-allowed; }
    .hint { text-align: center; color: var(--mute); font-size: 0.8rem; margin: 0.55rem 0 0; line-height: 1.35; }
    .how { padding: 1rem; border-radius: 16px; background: rgb(249 250 251 / 70%); border: 1px solid var(--line); }
    .how ol { margin: 0.55rem 0 0; padding-left: 1.15rem; color: var(--mute); font-size: 0.88rem; line-height: 1.45; }
    footer { color: var(--mute); font-size: 0.75rem; line-height: 1.4; }
  </style>
</head>
<body>
  <div class="phone">
    <header><span>KUI Residuals</span><span>Internal storage</span></header>
    <h1>Leftover folders from apps you already removed</h1>
    <p class="intro">Android often keeps an app’s private files after you uninstall it. This scan only looks. Nothing is moved or deleted until you say so.</p>
    <div class="counts">
      <div class="count"><b>${leftoverApps.size}</b><span>${if (leftoverApps.size == 1) "leftover app" else "leftover apps"}</span></div>
      <div class="count"><b>${needed.size}</b><span>still in use</span></div>
      <div class="count"><b>${ignored.size}</b><span>not leftovers</span></div>
    </div>
    <section>
      <h2>Leftovers</h2>
      <p class="lead">${esc(leftoverLead)}</p>
      $leftoverCards
      <button type="button" disabled>Set leftovers aside</button>
      <p class="hint">Next step is quarantine, not delete. You will see what would move before anything happens.</p>
    </section>
    <section>
      <h2>Still needed</h2>
      <p class="lead">These look like app folders, but the app is still on the phone, open, or part of Android. We will not offer them for cleanup.</p>
      $neededCards
    </section>
    <section>
      <h2>Not leftovers</h2>
      <p class="lead">If a name is not an app id, it is never treated as junk. Guessing here is how cleaners delete the wrong thing.</p>
      $ignoredCards
    </section>
    <section class="how">
      <h2>How we decide</h2>
      <ol>
        <li>Only first-level folders under Android/data, Android/obb, and Android/media.</li>
        <li>A leftover is an app id whose app is not installed, not running, and not system.</li>
        <li>Anything else stays “not a leftover.” We never promote it to leftover.</li>
      </ol>
    </section>
    <footer>Read-only preview of the scanner. Nothing was uploaded. The phone app comes after this core.</footer>
  </div>
</body>
</html>
""".trimIndent()
}

private fun nameOf(hit: ScanHit): String = hit.packageName ?: hit.path.canonical.substringAfterLast('/')

private fun shortPath(path: String): String =
    path.removePrefix("/storage/emulated/0/").ifBlank { path }

private fun placeTitle(path: String): String =
    when {
        "/Android/data/" in path -> "Private app files"
        "/Android/obb/" in path -> "Extra app packages"
        "/Android/media/" in path -> "App media"
        else -> "App folder"
    }

private fun neededKicker(hit: ScanHit): String =
    when {
        hit.reason.contains("installed", ignoreCase = true) -> "App still installed"
        hit.reason.contains("running", ignoreCase = true) -> "App is open"
        hit.reason.contains("system", ignoreCase = true) -> "Android system"
        else -> "Left alone"
    }

private fun neededWhy(hit: ScanHit): String =
    when {
        hit.reason.contains("installed", ignoreCase = true) ->
            "This app is still on the phone, so this folder is still in use."
        hit.reason.contains("running", ignoreCase = true) ->
            "This app is running right now. Touching it would break an open app."
        hit.reason.contains("system", ignoreCase = true) ->
            "This belongs to Android. We never mark system folders as leftovers."
        else -> hit.reason
    }

private fun ignoredWhy(hit: ScanHit): String =
    when {
        hit.reason.contains("Not a folder", ignoreCase = true) ->
            "This is a file, not a folder. Leftovers are folders named after apps."
        hit.reason.contains("Not a package", ignoreCase = true) ->
            "The name is not an app id (like com.example.app), so this is not leftover app data."
        else -> "We do not know what this is, so we will not treat it as leftover."
    }

private fun count(n: Int, noun: String): String = if (n == 1) "1 $noun" else "$n ${noun}s"

private fun esc(value: String): String =
    value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
