package kui.residuals.scanner

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PackageNameTest {
    @Test
    fun acceptsAndroidStyleNames() {
        assertTrue(looksLikePackageName("com.example.old"))
        assertTrue(looksLikePackageName("com.foo_bar.app2"))
    }

    @Test
    fun rejectsNonPackages() {
        assertFalse(looksLikePackageName("Cache"))
        assertFalse(looksLikePackageName(".thumbnails"))
        assertFalse(looksLikePackageName("com"))
        assertFalse(looksLikePackageName("123.foo"))
        assertFalse(looksLikePackageName(""))
    }
}
