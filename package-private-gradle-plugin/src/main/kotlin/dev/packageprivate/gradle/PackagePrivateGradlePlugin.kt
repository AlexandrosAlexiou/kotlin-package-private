package dev.packageprivate.gradle

import dev.packageprivate.gradle.analyzer.AnalyzePackagePrivateCandidatesTask
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/** Configuration extension for the package-private plugin. */
open class PackagePrivateExtension {
    /** Include public declarations in candidate analysis. Default: true */
    var includePublic: Boolean = true

    /** Include internal declarations in candidate analysis. Default: true */
    var includeInternal: Boolean = true

    /**
     * Output file for the analysis report. Default: build/reports/package-private-candidates.txt
     */
    var outputFile: File? = null
}

class PackagePrivateGradlePlugin : Plugin<Project> {
    companion object {
        private const val PLUGIN_VERSION = "1.2.0"
    }

    override fun apply(target: Project) {
        // Create extension
        val extension =
            target.extensions.create("packagePrivate", PackagePrivateExtension::class.java)

        // Add the annotations dependency automatically
        // For multiplatform projects, add to commonMain. For JVM projects, add to implementation.
        target.afterEvaluate {
            val kotlinExt = target.extensions.findByType(KotlinProjectExtension::class.java)
            if (kotlinExt is KotlinMultiplatformExtension) {
                // Multiplatform project - add to commonMain
                kotlinExt.sourceSets.findByName("commonMain")?.dependencies {
                    implementation("dev.packageprivate:package-private-annotations:$PLUGIN_VERSION")
                }
            } else {
                // JVM project - add to implementation configuration
                target.dependencies.add(
                    "implementation",
                    "dev.packageprivate:package-private-annotations:$PLUGIN_VERSION",
                )
            }

            registerAnalysisTask(target, extension)
        }
    }

    private fun registerAnalysisTask(project: Project, extension: PackagePrivateExtension) {
        project.tasks.register(
            "analyzePackagePrivateCandidates",
            AnalyzePackagePrivateCandidatesTask::class.java,
        ) { task ->
            // Collect all Kotlin source directories from the project
            val sourceDirectories = mutableSetOf<File>()

            // Try to get Kotlin extension (works for both JVM and multiplatform)
            project.extensions.findByType(KotlinProjectExtension::class.java)?.let { kotlinExt ->
                when (kotlinExt) {
                    is KotlinMultiplatformExtension -> {
                        // For multiplatform projects, collect from all source sets
                        kotlinExt.sourceSets.forEach { sourceSet ->
                            sourceSet.kotlin.srcDirs.forEach { srcDir ->
                                if (srcDir.exists()) {
                                    sourceDirectories.add(srcDir)
                                }
                            }
                        }
                    }
                    else -> {
                        // For JVM projects, use conventional directories
                        listOf(project.file("src/main/kotlin"), project.file("src/main/java"))
                            .filter { it.exists() }
                            .forEach { sourceDirectories.add(it) }
                    }
                }
            }

            // Fallback: if no source sets found, use conventional directories
            if (sourceDirectories.isEmpty()) {
                val fallbackDirs =
                    listOf(
                        project.file("src/main/kotlin"),
                        project.file("src/main/java"),
                        project.file("src/commonMain/kotlin"),
                    )
                fallbackDirs.filter { it.exists() }.forEach { sourceDirectories.add(it) }
            }

            // Add all discovered directories to the task
            sourceDirectories.forEach { srcDir ->
                task.sourceFiles.from(project.fileTree(srcDir) { it.include("**/*.kt") })
            }

            task.includePublic.set(extension.includePublic)
            task.includeInternal.set(extension.includeInternal)

            val outputFile =
                extension.outputFile
                    ?: project.file(
                        "${project.layout.buildDirectory.get()}/reports/package-private-candidates.txt"
                    )
            task.outputFile.set(outputFile)
        }
    }
}
