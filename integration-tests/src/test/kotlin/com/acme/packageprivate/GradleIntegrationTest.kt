package com.acme.packageprivate

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertContains

class GradleIntegrationTest {

    @TempDir
    lateinit var tempDir: File

    private val rootDir: File by lazy {
        var dir = File(System.getProperty("user.dir"))
        while (dir.name != "kotlin-package-private" && dir.parentFile != null) {
            dir = dir.parentFile
        }
        dir
    }

    @Test
    fun `gradle build succeeds with same package access`() {
        copyResourceProject("gradle-project", tempDir)
        // Remove the violating file for this test
        File(tempDir, "src/main/kotlin/com/example/api").deleteRecursively()

        val result = runGradle(tempDir, "build")
        assertTrue(result.exitCode == 0, "Build should succeed: ${result.output}")
    }

    @Test
    fun `gradle build fails with cross-package access`() {
        copyResourceProject("gradle-project", tempDir)

        val result = runGradle(tempDir, "build")
        assertTrue(result.exitCode != 0, "Build should fail")
        assertContains(result.output, "package-private")
    }

    @Test
    fun `java cannot access package-private kotlin class`() {
        copyResourceProject("java-interop-project", tempDir)

        val result = runGradle(tempDir, "build")
        // Java should fail to compile because KotlinInternal is now package-private in bytecode
        assertTrue(result.exitCode != 0, "Build should fail when Java accesses package-private Kotlin: ${result.output}")
        // Check for access error message (javac reports "not public" for package-private access)
        assertTrue(
            result.output.contains("not public") || 
            result.output.contains("KotlinInternal") ||
            result.output.contains("cannot be accessed"),
            "Error should mention access restriction: ${result.output}"
        )
    }

    @Test
    fun `java in same package can access package-private kotlin`() {
        copyResourceProject("java-interop-project", tempDir)
        
        // Move Java file to same package as Kotlin
        val javaSourceDir = File(tempDir, "src/main/java/com/example/internal")
        javaSourceDir.mkdirs()
        
        File(tempDir, "src/main/java/com/example/other").deleteRecursively()
        
        File(javaSourceDir, "JavaSamePackage.java").writeText("""
            package com.example.internal;
            
            // Same package - should be allowed
            public class JavaSamePackage {
                public String access() {
                    KotlinInternal internal = new KotlinInternal();
                    return internal.publicMethod();
                }
            }
        """.trimIndent())

        val result = runGradle(tempDir, "build")
        assertTrue(result.exitCode == 0, "Build should succeed for same package access: ${result.output}")
    }

    private fun copyResourceProject(name: String, targetDir: File) {
        // Copy gradle wrapper
        File(rootDir, "gradlew").copyTo(File(targetDir, "gradlew"), overwrite = true)
        File(targetDir, "gradlew").setExecutable(true)
        File(rootDir, "gradle").copyRecursively(File(targetDir, "gradle"), overwrite = true)

        // Copy resource project
        val resourceDir = File(javaClass.classLoader.getResource(name)!!.toURI())
        resourceDir.copyRecursively(targetDir, overwrite = true)
    }

    private fun runGradle(dir: File, vararg args: String): ProcessResult {
        val process = ProcessBuilder("./gradlew", *args, "--no-daemon", "-q")
            .directory(dir)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return ProcessResult(exitCode, output)
    }

    data class ProcessResult(val exitCode: Int, val output: String)
}
