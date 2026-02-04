package dev.packageprivate.analyzer.maven

import dev.packageprivate.analyzer.*
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File

/**
 * Maven Mojo that analyzes Kotlin source files to find candidates for @PackagePrivate annotation.
 * 
 * Usage:
 * ```
 * mvn dev.packageprivate:package-private-analyzer-maven-plugin:analyze
 * ```
 */
@Mojo(
    name = "analyze",
    defaultPhase = LifecyclePhase.VERIFY,
    threadSafe = true
)
class AnalyzePackagePrivateCandidatesMojo : AbstractMojo() {

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private var project: MavenProject? = null

    /**
     * Include public declarations in candidate analysis.
     */
    @Parameter(property = "packageprivate.analyzer.includePublic", defaultValue = "true")
    private var includePublic: Boolean = true

    /**
     * Include internal declarations in candidate analysis.
     */
    @Parameter(property = "packageprivate.analyzer.includeInternal", defaultValue = "true")
    private var includeInternal: Boolean = true

    /**
     * Output file for the analysis report.
     * Default: target/reports/package-private-candidates.txt
     */
    @Parameter(
        property = "packageprivate.analyzer.outputFile",
        defaultValue = "\${project.build.directory}/reports/package-private-candidates.txt"
    )
    private var outputFile: File? = null

    /**
     * Source directories to analyze. If not specified, defaults to src/main/kotlin and src/main/java.
     */
    @Parameter(property = "packageprivate.analyzer.sourceDirectories")
    private var sourceDirectories: List<File>? = null

    @Throws(MojoExecutionException::class)
    override fun execute() {
        val mavenProject = project ?: throw MojoExecutionException("Maven project not initialized")
        
        val sourceDirs = if (sourceDirectories == null || sourceDirectories!!.isEmpty()) {
            getDefaultSourceDirectories(mavenProject)
        } else {
            sourceDirectories!!
        }
        
        if (sourceDirs.isEmpty()) {
            log.warn("No source directories found to analyze")
            return
        }

        // Collect all Kotlin files from source directories
        val kotlinFiles = mutableListOf<File>()
        for (sourceDir in sourceDirs) {
            if (sourceDir.exists() && sourceDir.isDirectory) {
                sourceDir.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .forEach { kotlinFiles.add(it) }
            }
        }

        if (kotlinFiles.isEmpty()) {
            log.warn("No Kotlin source files found to analyze")
            return
        }

        log.info("Analyzing ${kotlinFiles.size} Kotlin files for @PackagePrivate candidates...")

        val projectRoot = mavenProject.basedir
        val analyzer = SourceAnalyzer()
        try {
            val analysisResult = analyzer.analyze(kotlinFiles, projectRoot)

            log.info("Found ${analysisResult.declarations.size} declarations and ${analysisResult.usages.size} usages")

            val finder = CandidateFinder(
                includePublic = includePublic,
                includeInternal = includeInternal
            )
            val candidates = finder.findCandidates(analysisResult)

            if (candidates.isEmpty()) {
                log.info("")
                log.info("✓ No candidates found for @PackagePrivate annotation")
                log.info("  All public/internal declarations are either:")
                log.info("  - Already annotated with @PackagePrivate")
                log.info("  - Used from multiple packages")
                log.info("  - Private visibility")
            } else {
                val report = buildReport(candidates)
                log.info(report)

                // Write to file if configured
                outputFile?.let { outFile ->
                    outFile.parentFile?.mkdirs()
                    outFile.writeText(report)
                    log.info("")
                    log.info("Report written to: ${outFile.absolutePath}")
                }
            }
        } catch (e: Exception) {
            throw MojoExecutionException("Failed to analyze source files", e)
        } finally {
            analyzer.dispose()
        }
    }

    private fun getDefaultSourceDirectories(mavenProject: MavenProject): List<File> {
        val dirs = mutableListOf<File>()
        
        // Try standard Maven Kotlin source directories
        val kotlinMain = File(mavenProject.basedir, "src/main/kotlin")
        if (kotlinMain.exists()) {
            dirs.add(kotlinMain)
        }
        
        val javaMain = File(mavenProject.basedir, "src/main/java")
        if (javaMain.exists()) {
            dirs.add(javaMain)
        }
        
        return dirs
    }

    private fun buildReport(candidates: List<Candidate>): String {
        return buildString {
            appendLine()
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine("  @PackagePrivate Candidates Report")
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine()
            appendLine("Found ${candidates.size} declaration(s) that could benefit from @PackagePrivate:")
            appendLine()

            // Group by package
            val byPackage = candidates.groupBy { it.declaration.packageName }

            for ((packageName, packageCandidates) in byPackage.toSortedMap()) {
                appendLine("Package: $packageName")
                appendLine("─".repeat(60))
                for (candidate in packageCandidates) {
                    append(candidate.format())
                }
                appendLine()
            }

            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine("  Summary: ${candidates.size} candidates found")
            appendLine("  - Classes: ${candidates.count { it.declaration.kind == DeclarationKind.CLASS }}")
            appendLine("  - Objects: ${candidates.count { it.declaration.kind == DeclarationKind.OBJECT }}")
            appendLine("  - Functions: ${candidates.count { it.declaration.kind == DeclarationKind.FUNCTION }}")
            appendLine("  - Properties: ${candidates.count { it.declaration.kind == DeclarationKind.PROPERTY }}")
            appendLine("═══════════════════════════════════════════════════════════════")
        }
    }
}
