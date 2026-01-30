package dev.packageprivate.gradle.analyzer

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.idea.KotlinFileType
import java.io.File

/**
 * Represents a declaration that could be a candidate for @PackagePrivate.
 */
data class Declaration(
    val fqName: String,
    val packageName: String,
    val name: String,
    val kind: DeclarationKind,
    val visibility: Visibility,
    val filePath: String,
    val line: Int,
    val hasPackagePrivateAnnotation: Boolean
)

enum class DeclarationKind {
    CLASS, FUNCTION, PROPERTY
}

enum class Visibility {
    PUBLIC, INTERNAL, PRIVATE, PROTECTED, UNKNOWN
}

/**
 * Represents a usage/reference to a declaration.
 */
data class Usage(
    val targetFqName: String,
    val callerPackage: String,
    val filePath: String,
    val line: Int
)

/**
 * Analyzes Kotlin source files to find declarations and their usages.
 */
@Suppress("DEPRECATION")
class SourceAnalyzer {
    
    private val disposable = Disposer.newDisposable()
    private val environment: KotlinCoreEnvironment
    
    init {
        val configuration = CompilerConfiguration().apply {
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        }
        environment = KotlinCoreEnvironment.createForProduction(
            disposable,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }
    
    fun dispose() {
        Disposer.dispose(disposable)
    }
    
    /**
     * Analyzes all Kotlin files and returns declarations and usages.
     */
    fun analyze(sourceFiles: List<File>): AnalysisResult {
        val declarations = mutableListOf<Declaration>()
        val usages = mutableListOf<Usage>()
        
        val ktFiles = sourceFiles.mapNotNull { file ->
            if (file.extension == "kt") {
                parseKotlinFile(file)
            } else null
        }
        
        // First pass: collect all declarations
        for (ktFile in ktFiles) {
            collectDeclarations(ktFile, declarations)
        }
        
        // Build a set of known declaration names for usage tracking
        val knownDeclarations = declarations.map { it.fqName }.toSet()
        
        // Second pass: collect usages
        for (ktFile in ktFiles) {
            collectUsages(ktFile, knownDeclarations, usages)
        }
        
        return AnalysisResult(declarations, usages)
    }
    
    private fun parseKotlinFile(file: File): KtFile? {
        return try {
            val content = file.readText()
            val virtualFile = LightVirtualFile(file.name, KotlinFileType.INSTANCE, content)
            PsiManager.getInstance(environment.project).findFile(virtualFile) as? KtFile
        } catch (e: Exception) {
            null
        }
    }
    
    private fun collectDeclarations(ktFile: KtFile, declarations: MutableList<Declaration>) {
        val packageName = ktFile.packageFqName.asString()
        val filePath = ktFile.name
        
        ktFile.accept(object : KtTreeVisitorVoid() {
            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)
                val name = klass.name ?: return
                val fqName = if (packageName.isNotEmpty()) "$packageName.$name" else name
                
                declarations.add(Declaration(
                    fqName = fqName,
                    packageName = packageName,
                    name = name,
                    kind = DeclarationKind.CLASS,
                    visibility = klass.visibilityModifier(),
                    filePath = filePath,
                    line = ktFile.getLineNumber(klass.textOffset) + 1,
                    hasPackagePrivateAnnotation = klass.hasPackagePrivateAnnotation()
                ))
            }
            
            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                val name = function.name ?: return
                // Skip local functions
                if (function.isLocal) return
                
                val containingClass = function.parent?.parent as? KtClassOrObject
                val fqName = when {
                    containingClass != null -> {
                        val className = containingClass.fqName?.asString() ?: return
                        "$className.$name"
                    }
                    packageName.isNotEmpty() -> "$packageName.$name"
                    else -> name
                }
                
                declarations.add(Declaration(
                    fqName = fqName,
                    packageName = packageName,
                    name = name,
                    kind = DeclarationKind.FUNCTION,
                    visibility = function.visibilityModifier(),
                    filePath = filePath,
                    line = ktFile.getLineNumber(function.textOffset) + 1,
                    hasPackagePrivateAnnotation = function.hasPackagePrivateAnnotation()
                ))
            }
            
            override fun visitProperty(property: KtProperty) {
                super.visitProperty(property)
                val name = property.name ?: return
                // Skip local properties
                if (property.isLocal) return
                
                val containingClass = property.parent?.parent as? KtClassOrObject
                val fqName = when {
                    containingClass != null -> {
                        val className = containingClass.fqName?.asString() ?: return
                        "$className.$name"
                    }
                    packageName.isNotEmpty() -> "$packageName.$name"
                    else -> name
                }
                
                declarations.add(Declaration(
                    fqName = fqName,
                    packageName = packageName,
                    name = name,
                    kind = DeclarationKind.PROPERTY,
                    visibility = property.visibilityModifier(),
                    filePath = filePath,
                    line = ktFile.getLineNumber(property.textOffset) + 1,
                    hasPackagePrivateAnnotation = property.hasPackagePrivateAnnotation()
                ))
            }
        })
    }
    
    private fun collectUsages(ktFile: KtFile, knownDeclarations: Set<String>, usages: MutableList<Usage>) {
        val callerPackage = ktFile.packageFqName.asString()
        val filePath = ktFile.name
        
        // Collect imports to resolve simple names
        val imports = mutableMapOf<String, String>() // simpleName -> fqName
        for (importDirective in ktFile.importDirectives) {
            val importedFqName = importDirective.importedFqName?.asString() ?: continue
            val simpleName = importedFqName.substringAfterLast('.')
            imports[simpleName] = importedFqName
        }
        
        ktFile.accept(object : KtTreeVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)
                val callee = expression.calleeExpression?.text ?: return
                
                // Try to resolve the called name
                val fqName = resolveToFqName(callee, imports, callerPackage, knownDeclarations)
                if (fqName != null && fqName in knownDeclarations) {
                    usages.add(Usage(
                        targetFqName = fqName,
                        callerPackage = callerPackage,
                        filePath = filePath,
                        line = ktFile.getLineNumber(expression.textOffset) + 1
                    ))
                }
            }
            
            override fun visitReferenceExpression(expression: KtReferenceExpression) {
                super.visitReferenceExpression(expression)
                if (expression is KtCallExpression) return // Already handled
                
                val name = expression.text
                val fqName = resolveToFqName(name, imports, callerPackage, knownDeclarations)
                if (fqName != null && fqName in knownDeclarations) {
                    usages.add(Usage(
                        targetFqName = fqName,
                        callerPackage = callerPackage,
                        filePath = filePath,
                        line = ktFile.getLineNumber(expression.textOffset) + 1
                    ))
                }
            }
        })
    }
    
    private fun resolveToFqName(
        name: String,
        imports: Map<String, String>,
        currentPackage: String,
        knownDeclarations: Set<String>
    ): String? {
        // Check if it's already a fully qualified name
        if (name in knownDeclarations) return name
        
        // Check imports
        val importedFqName = imports[name]
        if (importedFqName != null && importedFqName in knownDeclarations) {
            return importedFqName
        }
        
        // Check same package
        val samePackageFqName = if (currentPackage.isNotEmpty()) "$currentPackage.$name" else name
        if (samePackageFqName in knownDeclarations) {
            return samePackageFqName
        }
        
        return null
    }
    
    private fun KtModifierListOwner.visibilityModifier(): Visibility {
        return when {
            hasModifier(org.jetbrains.kotlin.lexer.KtTokens.PRIVATE_KEYWORD) -> Visibility.PRIVATE
            hasModifier(org.jetbrains.kotlin.lexer.KtTokens.INTERNAL_KEYWORD) -> Visibility.INTERNAL
            hasModifier(org.jetbrains.kotlin.lexer.KtTokens.PROTECTED_KEYWORD) -> Visibility.PROTECTED
            hasModifier(org.jetbrains.kotlin.lexer.KtTokens.PUBLIC_KEYWORD) -> Visibility.PUBLIC
            else -> Visibility.PUBLIC // Default in Kotlin is public
        }
    }
    
    private fun KtAnnotated.hasPackagePrivateAnnotation(): Boolean {
        return annotationEntries.any { annotation ->
            val name = annotation.shortName?.asString()
            name == "PackagePrivate"
        }
    }
    
    private fun KtFile.getLineNumber(offset: Int): Int {
        val text = this.text
        var line = 0
        for (i in 0 until offset.coerceAtMost(text.length)) {
            if (text[i] == '\n') line++
        }
        return line
    }
}

data class AnalysisResult(
    val declarations: List<Declaration>,
    val usages: List<Usage>
)
