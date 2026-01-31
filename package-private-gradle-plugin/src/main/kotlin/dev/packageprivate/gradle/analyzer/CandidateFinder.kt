package dev.packageprivate.gradle.analyzer

/**
 * Finds declarations that are candidates for @PackagePrivate annotation.
 * 
 * A declaration is a candidate if:
 * 1. It is public or internal (not already private)
 * 2. It does NOT already have @PackagePrivate annotation
 * 3. It is ONLY used within its own package (no cross-package usages)
 */
class CandidateFinder(
    private val includePublic: Boolean = true,
    private val includeInternal: Boolean = true
) {
    
    /**
     * Finds candidates from the analysis result.
     */
    fun findCandidates(analysisResult: AnalysisResult): List<Candidate> {
        val (declarations, usages) = analysisResult
        
        // Build usage map: declaration fqName -> set of packages that use it
        val usagesByDeclaration = usages.groupBy { it.targetFqName }
            .mapValues { (_, usageList) -> usageList.map { it.callerPackage }.toSet() }
        
        return declarations.mapNotNull { declaration ->
            // Skip if already has @PackagePrivate
            if (declaration.hasPackagePrivateAnnotation) return@mapNotNull null
            
            // Skip if containing class has @PackagePrivate (member is already implicitly package-private)
            if (declaration.containingClassHasPackagePrivate) return@mapNotNull null
            
            // Skip if private (already restricted)
            if (declaration.visibility == Visibility.PRIVATE) return@mapNotNull null
            
            // Skip if protected (different semantics)
            if (declaration.visibility == Visibility.PROTECTED) return@mapNotNull null
            
            // Check configuration
            if (declaration.visibility == Visibility.PUBLIC && !includePublic) return@mapNotNull null
            if (declaration.visibility == Visibility.INTERNAL && !includeInternal) return@mapNotNull null
            
            // Get all packages that use this declaration
            val usedInPackages = usagesByDeclaration[declaration.fqName] ?: emptySet()
            
            // Filter out same-package usages to see if there are cross-package usages
            val crossPackageUsages = usedInPackages.filter { it != declaration.packageName }
            
            // If NO cross-package usages, this is a candidate
            if (crossPackageUsages.isEmpty()) {
                Candidate(
                    declaration = declaration,
                    usedOnlyInPackage = declaration.packageName,
                    usageCount = usagesByDeclaration[declaration.fqName]?.size ?: 0
                )
            } else {
                null
            }
        }
    }
}

/**
 * A declaration that is a candidate for @PackagePrivate.
 */
data class Candidate(
    val declaration: Declaration,
    val usedOnlyInPackage: String,
    val usageCount: Int
) {
    fun format(): String {
        val kindStr = when (declaration.kind) {
            DeclarationKind.CLASS -> "class"
            DeclarationKind.OBJECT -> "object"
            DeclarationKind.FUNCTION -> "function"
            DeclarationKind.PROPERTY -> "property"
            DeclarationKind.ENUM_CLASS -> "enum class"
            DeclarationKind.SEALED_CLASS -> "sealed class"
            DeclarationKind.SEALED_INTERFACE -> "sealed interface"
            DeclarationKind.TYPEALIAS -> "typealias"
        }
        val visibilityStr = when (declaration.visibility) {
            Visibility.PUBLIC -> "public"
            Visibility.INTERNAL -> "internal"
            else -> declaration.visibility.name.lowercase()
        }
        return buildString {
            appendLine("${declaration.fqName} ($kindStr)")
            appendLine("  └── Only used in package: $usedOnlyInPackage")
            appendLine("  └── Current visibility: $visibilityStr")
            appendLine("  └── Location: ${declaration.filePath}:${declaration.line}")
        }
    }
}
