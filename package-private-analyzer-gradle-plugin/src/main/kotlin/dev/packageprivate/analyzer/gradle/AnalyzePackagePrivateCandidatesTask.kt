package dev.packageprivate.analyzer.gradle

import dev.packageprivate.analyzer.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

/**
 * Gradle task that analyzes Kotlin source files to find candidates for @PackagePrivate annotation.
 * 
 * Usage:
 * ```
 * ./gradlew analyzePackagePrivateCandidates
 * ```
 */
abstract class AnalyzePackagePrivateCandidatesTask : DefaultTask() {
    
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection
    
    @get:Input
    abstract val includePublic: Property<Boolean>
    
    @get:Input
    abstract val includeInternal: Property<Boolean>
    
    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty
    
    init {
        group = "verification"
        description = "Analyzes Kotlin sources to find candidates for @PackagePrivate annotation"
        
        includePublic.convention(true)
        includeInternal.convention(true)
    }
    
    @TaskAction
    fun analyze() {
        val files = sourceFiles.files.filter { it.extension == "kt" && it.exists() }
        
        if (files.isEmpty()) {
            logger.warn("No Kotlin source files found to analyze")
            return
        }
        
        logger.lifecycle("Analyzing ${files.size} Kotlin files for @PackagePrivate candidates...")
        
        val projectRoot = project.rootProject.projectDir
        val analyzer = SourceAnalyzer()
        try {
            val analysisResult = analyzer.analyze(files.toList(), projectRoot)
            
            logger.lifecycle("Found ${analysisResult.declarations.size} declarations and ${analysisResult.usages.size} usages")
            
            val finder = CandidateFinder(
                includePublic = includePublic.get(),
                includeInternal = includeInternal.get()
            )
            val candidates = finder.findCandidates(analysisResult)
            
            if (candidates.isEmpty()) {
                logger.lifecycle("\n✓ No candidates found for @PackagePrivate annotation")
                logger.lifecycle("  All public/internal declarations are either:")
                logger.lifecycle("  - Already annotated with @PackagePrivate")
                logger.lifecycle("  - Used from multiple packages")
                logger.lifecycle("  - Private visibility")
            } else {
                val report = buildReport(candidates)
                logger.lifecycle(report)
                
                // Write to file if configured
                if (outputFile.isPresent) {
                    val outFile = outputFile.get().asFile
                    outFile.parentFile?.mkdirs()
                    outFile.writeText(report)
                    logger.lifecycle("\nReport written to: ${outFile.absolutePath}")
                }
            }
        } finally {
            analyzer.dispose()
        }
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
