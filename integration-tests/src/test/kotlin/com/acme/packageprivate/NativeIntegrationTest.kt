package com.acme.packageprivate

import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class NativeIntegrationTest {

    @TempDir lateinit var tempDir: File

    private val rootDir: File by lazy {
        var dir = File(System.getProperty("user.dir"))
        while (dir.name != "kotlin-package-private" && dir.parentFile != null) {
            dir = dir.parentFile
        }
        dir
    }

    @Test
    fun `native build succeeds with same package access`() {
        copyResourceProject("native-same-package-project", tempDir)

        val result = runGradle(tempDir, "compileKotlinNative")
        assertEquals(result.exitCode, 0, "Build should succeed: ${result.output}")
    }

    @Test
    fun `native build fails with cross-package access`() {
        copyResourceProject("native-project", tempDir)

        val result = runGradle(tempDir, "compileKotlinNative")
        assertTrue(result.exitCode != 0, "Build should fail: ${result.output}")
        assertContains(result.output, "package-private")
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
            ProcessBuilder("./gradlew", *args, "--no-daemon")
                .directory(dir)
                .redirectErrorStream(true)
                .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return ProcessResult(exitCode, output)
    }

    data class ProcessResult(val exitCode: Int, val output: String)
}
