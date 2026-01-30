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
    CLASS, OBJECT, FUNCTION, PROPERTY, ENUM_CLASS, SEALED_CLASS, SEALED_INTERFACE, TYPEALIAS
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
            
            private fun getContainingFqNamePrefix(element: org.jetbrains.kotlin.com.intellij.psi.PsiElement): String? {
                var parent = element.parent
                while (parent != null) {
                    when (parent) {
                        is KtClassOrObject -> {
                            val containerFqName = parent.fqName?.asString()
                            if (containerFqName != null) return containerFqName
                        }
                    }
                    parent = parent.parent
                }
                return null
            }
            
            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)
                val name = klass.name ?: return
                
                // Determine kind based on class type
                val kind = when {
                    klass.isEnum() -> DeclarationKind.ENUM_CLASS
                    klass.isSealed() && klass.isInterface() -> DeclarationKind.SEALED_INTERFACE
                    klass.isSealed() -> DeclarationKind.SEALED_CLASS
                    else -> DeclarationKind.CLASS
                }
                
                // Handle nested classes
                val containerFqName = getContainingFqNamePrefix(klass)
                val fqName = when {
                    containerFqName != null -> "$containerFqName.$name"
                    packageName.isNotEmpty() -> "$packageName.$name"
                    else -> name
                }
                
                declarations.add(Declaration(
                    fqName = fqName,
                    packageName = packageName,
                    name = name,
                    kind = kind,
                    visibility = klass.visibilityModifier(),
                    filePath = filePath,
                    line = ktFile.getLineNumber(klass.textOffset) + 1,
                    hasPackagePrivateAnnotation = klass.hasPackagePrivateAnnotation()
                ))
            }
            
            override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
                super.visitObjectDeclaration(declaration)
                // Skip companion objects (they're part of the containing class)
                if (declaration.isCompanion()) return
                
                val name = declaration.name ?: return
                
                // Handle nested objects
                val containerFqName = getContainingFqNamePrefix(declaration)
                val fqName = when {
                    containerFqName != null -> "$containerFqName.$name"
                    packageName.isNotEmpty() -> "$packageName.$name"
                    else -> name
                }
                
                declarations.add(Declaration(
                    fqName = fqName,
                    packageName = packageName,
                    name = name,
                    kind = DeclarationKind.OBJECT,
                    visibility = declaration.visibilityModifier(),
                    filePath = filePath,
                    line = ktFile.getLineNumber(declaration.textOffset) + 1,
                    hasPackagePrivateAnnotation = declaration.hasPackagePrivateAnnotation()
                ))
            }
            
            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                val name = function.name ?: return
                // Skip local functions
                if (function.isLocal) return
                
                // Check if it's an extension function - include receiver type in fqName
                val receiverType = function.receiverTypeReference?.text
                val functionName = if (receiverType != null) "$receiverType.$name" else name
                
                val containerFqName = getContainingFqNamePrefix(function)
                val fqName = when {
                    containerFqName != null -> "$containerFqName.$functionName"
                    packageName.isNotEmpty() -> "$packageName.$functionName"
                    else -> functionName
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
                
                // Check if it's an extension property - include receiver type in fqName
                val receiverType = property.receiverTypeReference?.text
                val propertyName = if (receiverType != null) "$receiverType.$name" else name
                
                val containerFqName = getContainingFqNamePrefix(property)
                val fqName = when {
                    containerFqName != null -> "$containerFqName.$propertyName"
                    packageName.isNotEmpty() -> "$packageName.$propertyName"
                    else -> propertyName
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
            
            override fun visitTypeAlias(typeAlias: KtTypeAlias) {
                super.visitTypeAlias(typeAlias)
                val name = typeAlias.name ?: return
                val fqName = if (packageName.isNotEmpty()) "$packageName.$name" else name
                
                declarations.add(Declaration(
                    fqName = fqName,
                    packageName = packageName,
                    name = name,
                    kind = DeclarationKind.TYPEALIAS,
                    visibility = typeAlias.visibilityModifier(),
                    filePath = filePath,
                    line = ktFile.getLineNumber(typeAlias.textOffset) + 1,
                    hasPackagePrivateAnnotation = typeAlias.hasPackagePrivateAnnotation()
                ))
            }
        })
    }
    
    private fun collectUsages(ktFile: KtFile, knownDeclarations: Set<String>, usages: MutableList<Usage>) {
        val callerPackage = ktFile.packageFqName.asString()
        val filePath = ktFile.name
        
        // Collect imports to resolve simple names
        val imports = mutableMapOf<String, String>() // simpleName -> fqName
        val starImportPackages = mutableListOf<String>() // packages from star imports
        
        for (importDirective in ktFile.importDirectives) {
            if (importDirective.isAllUnder) {
                // Star import: import com.example.*
                val packageFqName = importDirective.importedFqName?.asString() ?: continue
                starImportPackages.add(packageFqName)
            } else {
                val importedFqName = importDirective.importedFqName?.asString() ?: continue
                val simpleName = importedFqName.substringAfterLast('.')
                imports[simpleName] = importedFqName
            }
        }
        
        ktFile.accept(object : KtTreeVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)
                val callee = expression.calleeExpression?.text ?: return
                
                // Try to resolve the called name
                val fqName = resolveToFqName(callee, imports, starImportPackages, callerPackage, knownDeclarations)
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
                val fqName = resolveToFqName(name, imports, starImportPackages, callerPackage, knownDeclarations)
                if (fqName != null && fqName in knownDeclarations) {
                    usages.add(Usage(
                        targetFqName = fqName,
                        callerPackage = callerPackage,
                        filePath = filePath,
                        line = ktFile.getLineNumber(expression.textOffset) + 1
                    ))
                }
            }
            
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                super.visitDotQualifiedExpression(expression)
                // Handle qualified references like com.example.Helper
                // For calls like com.example.Helper(), we want to extract com.example.Helper
                val selectorExpr = expression.selectorExpression
                val receiverExpr = expression.receiverExpression
                
                // Build the qualified name without call arguments
                val qualifiedName = when (selectorExpr) {
                    is KtCallExpression -> {
                        val callName = selectorExpr.calleeExpression?.text ?: return
                        "${receiverExpr.text}.$callName"
                    }
                    else -> expression.text
                }
                
                // Check if the qualified name matches a known declaration
                if (qualifiedName in knownDeclarations) {
                    usages.add(Usage(
                        targetFqName = qualifiedName,
                        callerPackage = callerPackage,
                        filePath = filePath,
                        line = ktFile.getLineNumber(expression.textOffset) + 1
                    ))
                }
            }
            
            override fun visitTypeReference(typeReference: KtTypeReference) {
                super.visitTypeReference(typeReference)
                // Handle type references like: val x: Helper, fun foo(): Helper
                val typeText = typeReference.text
                // Remove nullability markers for lookup
                val cleanType = typeText.replace("?", "").trim()
                
                // Handle the main type (before generics)
                val mainType = cleanType.substringBefore("<").trim()
                val fqName = resolveToFqName(mainType, imports, starImportPackages, callerPackage, knownDeclarations)
                if (fqName != null && fqName in knownDeclarations) {
                    usages.add(Usage(
                        targetFqName = fqName,
                        callerPackage = callerPackage,
                        filePath = filePath,
                        line = ktFile.getLineNumber(typeReference.textOffset) + 1
                    ))
                }
                
                // Handle generic type parameters: List<Helper>, Map<String, Helper>
                if (cleanType.contains("<")) {
                    val genericsPart = cleanType.substringAfter("<").substringBeforeLast(">")
                    // Split by comma, but be careful with nested generics
                    extractTypeNames(genericsPart).forEach { typeName ->
                        val genericFqName = resolveToFqName(typeName, imports, starImportPackages, callerPackage, knownDeclarations)
                        if (genericFqName != null && genericFqName in knownDeclarations) {
                            usages.add(Usage(
                                targetFqName = genericFqName,
                                callerPackage = callerPackage,
                                filePath = filePath,
                                line = ktFile.getLineNumber(typeReference.textOffset) + 1
                            ))
                        }
                    }
                }
            }
            
            override fun visitSuperTypeList(list: KtSuperTypeList) {
                super.visitSuperTypeList(list)
                // Handle inheritance: class Foo : BaseClass(), Interface
                for (entry in list.entries) {
                    val typeRef = entry.typeReference ?: continue
                    val typeText = typeRef.text.replace("?", "").substringBefore("<").trim()
                    
                    val fqName = resolveToFqName(typeText, imports, starImportPackages, callerPackage, knownDeclarations)
                    if (fqName != null && fqName in knownDeclarations) {
                        usages.add(Usage(
                            targetFqName = fqName,
                            callerPackage = callerPackage,
                            filePath = filePath,
                            line = ktFile.getLineNumber(entry.textOffset) + 1
                        ))
                    }
                }
            }
            
            override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
                super.visitAnnotationEntry(annotationEntry)
                // Handle annotation usage: @MyAnnotation
                val annotationName = annotationEntry.shortName?.asString() ?: return
                
                val fqName = resolveToFqName(annotationName, imports, starImportPackages, callerPackage, knownDeclarations)
                if (fqName != null && fqName in knownDeclarations) {
                    usages.add(Usage(
                        targetFqName = fqName,
                        callerPackage = callerPackage,
                        filePath = filePath,
                        line = ktFile.getLineNumber(annotationEntry.textOffset) + 1
                    ))
                }
            }
        })
    }
    
    /**
     * Extracts type names from a generic parameter string like "String, Helper" or "K, V"
     * Handles nested generics like "List<Helper>, Map<String, Other>"
     */
    private fun extractTypeNames(generics: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var current = StringBuilder()
        
        for (char in generics) {
            when {
                char == '<' -> {
                    depth++
                    current.append(char)
                }
                char == '>' -> {
                    depth--
                    current.append(char)
                }
                char == ',' && depth == 0 -> {
                    val typeName = current.toString().trim().substringBefore("<").replace("?", "").trim()
                    if (typeName.isNotEmpty()) result.add(typeName)
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        
        // Add the last type
        val lastType = current.toString().trim().substringBefore("<").replace("?", "").trim()
        if (lastType.isNotEmpty()) result.add(lastType)
        
        return result
    }
    
    private fun resolveToFqName(
        name: String,
        imports: Map<String, String>,
        starImportPackages: List<String>,
        currentPackage: String,
        knownDeclarations: Set<String>
    ): String? {
        // Check if it's already a fully qualified name
        if (name in knownDeclarations) return name
        
        // Check explicit imports
        val importedFqName = imports[name]
        if (importedFqName != null && importedFqName in knownDeclarations) {
            return importedFqName
        }
        
        // Check same package
        val samePackageFqName = if (currentPackage.isNotEmpty()) "$currentPackage.$name" else name
        if (samePackageFqName in knownDeclarations) {
            return samePackageFqName
        }
        
        // Check star imports
        for (starPackage in starImportPackages) {
            val starFqName = "$starPackage.$name"
            if (starFqName in knownDeclarations) {
                return starFqName
            }
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
