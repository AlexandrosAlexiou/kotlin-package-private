package dev.packageprivate.gradle

import dev.packageprivate.gradle.analyzer.AnalyzePackagePrivateCandidatesTask
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import java.io.File

/**
 * Configuration extension for the package-private plugin.
 */
open class PackagePrivateExtension {
    /** Include public declarations in candidate analysis. Default: true */
    var includePublic: Boolean = true
    
    /** Include internal declarations in candidate analysis. Default: true */
    var includeInternal: Boolean = true
    
    /** Output file for the analysis report. Default: build/reports/package-private-candidates.txt */
    var outputFile: File? = null
}

class PackagePrivateGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        // Create extension
        val extension = target.extensions.create("packagePrivate", PackagePrivateExtension::class.java)
        
        // Add the annotations dependency automatically
        target.dependencies.add(
            "implementation",
            "com.acme:package-private-annotations:${target.rootProject.version}",
        )
        
        // Register the analysis task
        target.afterEvaluate {
            registerAnalysisTask(target, extension)
        }
    }
    
    private fun registerAnalysisTask(project: Project, extension: PackagePrivateExtension) {
        project.tasks.register("analyzePackagePrivateCandidates", AnalyzePackagePrivateCandidatesTask::class.java) { task ->
            // Collect source files from src/main/kotlin and src/main/java directories
            val srcDirs = listOf(
                project.file("src/main/kotlin"),
                project.file("src/main/java"),
                project.file("src/commonMain/kotlin"),
                project.file("src/jvmMain/kotlin")
            )
            
            srcDirs.filter { it.exists() }.forEach { srcDir ->
                task.sourceFiles.from(project.fileTree(srcDir) {
                    it.include("**/*.kt")
                })
            }
            
            task.includePublic.set(extension.includePublic)
            task.includeInternal.set(extension.includeInternal)
            
            val outputFile = extension.outputFile 
                ?: project.file("${project.layout.buildDirectory.get()}/reports/package-private-candidates.txt")
            task.outputFile.set(outputFile)
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = "dev.packageprivate.package-private"

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(
            groupId = "com.acme",
            artifactId = "package-private-compiler-plugin",
            version = "0.1.0",
        )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        return kotlinCompilation.target.project.provider { emptyList() }
    }
}
