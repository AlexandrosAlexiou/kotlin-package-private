package dev.packageprivate

import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class AnalyzerMavenIntegrationTest {

    @TempDir lateinit var tempDir: File

    @Test
    fun `maven analyzer finds candidates in same package`() {
        copyResourceProject("maven-analyzer-project", tempDir)

        val result = runMaven(tempDir, "dev.packageprivate.analyzer:package-private-analyzer-maven-plugin:1.3.0:analyze")
        assertEquals(0, result.exitCode, "Analyzer should succeed: ${result.output}")
        
        // Should find candidates: InternalHelper, InternalService, utilityFunction
        assertContains(result.output, "InternalHelper")
        assertContains(result.output, "InternalService")
        assertContains(result.output, "utilityFunction")
        
        // Should show they're only used in their packages
        assertContains(result.output, "Only used in package:")
        
        // Should find multiple candidates
        assertContains(result.output, "candidates found")
    }

    private fun copyResourceProject(name: String, targetDir: File) {
        val resourceDir = File(javaClass.classLoader.getResource(name)!!.toURI())
        resourceDir.copyRecursively(targetDir, overwrite = true)
    }

    private fun runMaven(dir: File, vararg args: String): ProcessResult {
        val process =
            ProcessBuilder("mvn", "clean", "compile", *args)
                .directory(dir)
                .redirectErrorStream(true)
                .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return ProcessResult(exitCode, output)
    }

    data class ProcessResult(val exitCode: Int, val output: String)
}
