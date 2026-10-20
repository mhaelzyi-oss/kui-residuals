package kui.residuals.access

class FakeFileOps(
    private val links: Map<String, String> = emptyMap(),
) : FileOps {
    override fun canonicalize(path: String): String {
        val collapsed = PathGuard.collapse(path)
        val target = links[collapsed] ?: return collapsed
        return PathGuard.collapse(target)
    }

    override fun isSymbolicLink(path: String): Boolean = links.containsKey(PathGuard.collapse(path))
}

class MapDocumentTree(
    private val children: Map<String, List<Dirent>>,
) : DocumentTree {
    override fun list(canonical: String): List<Dirent> = children[canonical].orEmpty()
}

class ScriptedShell(
    private val answers: Map<String, ShellResult>,
) : ShellSession {
    override fun exec(command: String): ShellResult =
        answers[command] ?: ShellResult(code = 1, stdout = "", stderr = "unexpected: $command")
}
