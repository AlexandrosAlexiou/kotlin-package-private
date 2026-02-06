package dev.packageprivate

import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class AnalyzerGradleIntegrationTest {

    @TempDir lateinit var tempDir: File

    private val rootDir: File by lazy {
        var dir = File(System.getProperty("user.dir"))
        while (dir.name != "package-private" && dir.parentFile != null) {
            dir = dir.parentFile
        }
        dir
    }

    @Test
    fun `analyzer finds candidates in same package`() {
        copyResourceProject("gradle-analyzer-project", tempDir)

        val result = runGradle(tempDir, "analyzePackagePrivateCandidates")
        assertEquals(0, result.exitCode, "Analyzer should succeed: ${result.output}")

        // Should find candidates: InternalHelper, InternalService, utilityFunction
        assertContains(result.output, "InternalHelper")
        assertContains(result.output, "InternalService")
        assertContains(result.output, "utilityFunction")

        // Should show they're only used in com.example.internal
        assertContains(result.output, "Only used in package:")

        // Should find multiple candidates
        assertContains(result.output, "candidates found")
    }

    @Test
    fun `analyzer excludes main function from candidates`() {
        copyResourceProject("gradle-analyzer-project", tempDir)

        val result = runGradle(tempDir, "analyzePackagePrivateCandidates")
        assertEquals(0, result.exitCode, "Analyzer should succeed: ${result.output}")

        assertContains(result.output, "candidates found")

        val mainPattern = Regex("""com\.example\.main\s*\(function\)""")
        assertEquals(
            false,
            mainPattern.containsMatchIn(result.output),
            "main function should not be reported as a candidate",
        )
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
        val process =
            ProcessBuilder("./gradlew", *args, "--no-daemon", "--stacktrace")
                .directory(dir)
                .redirectErrorStream(true)
                .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return ProcessResult(exitCode, output)
    }

    data class ProcessResult(val exitCode: Int, val output: String)
}
