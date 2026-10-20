package kui.residuals.access

import java.util.Base64

internal fun ShellSession.mkdirp(path: GuardedPath) {
    val result = exec("mkdir -p -- '${path.canonical}'")
    if (result.code != 0) throw AccessDenied(result.stderr.ifBlank { "Could not create folder." })
}

internal fun ShellSession.moveInto(from: GuardedPath, to: GuardedPath) {
    val result = exec("mv -- '${from.canonical}' '${to.canonical}'")
    if (result.code != 0) throw AccessDenied(result.stderr.ifBlank { "Could not move folder." })
}

internal fun ShellSession.copyInto(from: GuardedPath, to: GuardedPath) {
    val result = exec("cp -a -- '${from.canonical}' '${to.canonical}'")
    if (result.code != 0) throw AccessDenied(result.stderr.ifBlank { "Could not save a copy." })
}

internal fun ShellSession.removeTree(path: GuardedPath) {
    if (path.root != RootKind.SET_ASIDE) {
        throw AccessDenied("Delete is only for folders already set aside.")
    }
    val result = exec("rm -rf -- '${path.canonical}'")
    if (result.code != 0) throw AccessDenied(result.stderr.ifBlank { "Could not delete." })
}

internal fun ShellSession.catFile(path: GuardedPath): ByteArray {
    val result = exec("cat -- '${path.canonical}'")
    if (result.code != 0) throw AccessDenied(result.stderr.ifBlank { "Could not read." })
    return result.stdout.toByteArray()
}

internal fun ShellSession.putFile(path: GuardedPath, bytes: ByteArray) {
    if (path.canonical.contains('\'')) throw AccessDenied("Could not write.")
    val encoded = Base64.getEncoder().encodeToString(bytes)
    val result = exec("echo '$encoded' | base64 -d > '${path.canonical}'")
    if (result.code != 0) throw AccessDenied(result.stderr.ifBlank { "Could not write." })
}
