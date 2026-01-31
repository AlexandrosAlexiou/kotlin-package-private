package dev.packageprivate.compiler

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirCallableDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.abbreviatedTypeOrSelf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class PackagePrivateChecker(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val callCheckers: Set<FirCallChecker> = setOf(PackagePrivateCallChecker)
    }
    
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val callableDeclarationCheckers: Set<FirCallableDeclarationChecker> = 
            setOf(RedundantPackagePrivateChecker)
    }
    
    override val typeCheckers: TypeCheckers = object : TypeCheckers() {
        override val resolvedTypeRefCheckers: Set<FirResolvedTypeRefChecker> = 
            setOf(PackagePrivateTypeAliasChecker)
    }
}

private val PACKAGE_PRIVATE_CLASS_ID = ClassId(
    FqName("dev.packageprivate"),
    org.jetbrains.kotlin.name.Name.identifier("PackagePrivate")
)

private fun FirAnnotation.hasPackagePrivateClassId(): Boolean {
    val typeRef = this.annotationTypeRef as? FirResolvedTypeRef ?: return false
    return typeRef.coneType.classId == PACKAGE_PRIVATE_CLASS_ID
}

@OptIn(SymbolInternals::class)
private object PackagePrivateCallChecker : FirCallChecker(MppCheckerKind.Common) {
    context(ctx: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCall) {
        val fileSymbol = ctx.containingFileSymbol ?: return
        val callerPackage = fileSymbol.fir.packageDirective.packageFqName

        val resolvable = expression as? FirResolvable ?: return
        val symbol = resolvable.calleeReference.toResolvedCallableSymbol() ?: return

        // Check if the called symbol itself is package-private
        checkSymbol(symbol, callerPackage, expression)

        // For constructors, also check if the containing class is package-private
        if (symbol is FirConstructorSymbol) {
            val returnType = symbol.resolvedReturnType
            val classSymbol = returnType.toRegularClassSymbol() ?: return
            
            val annotation = classSymbol.annotations.firstOrNull { it.hasPackagePrivateClassId() }
            if (annotation != null) {
                val scopeOverride = extractScopeFromAnnotation(annotation)
                val targetPackage = if (scopeOverride.isNotEmpty()) {
                    FqName(scopeOverride)
                } else {
                    classSymbol.classId.packageFqName
                }
                if (callerPackage != targetPackage) {
                    val source = expression.source ?: return
                    reporter.reportOn(
                        source,
                        PackagePrivateErrors.PACKAGE_PRIVATE_ACCESS,
                        classSymbol.classId.asSingleFqName().asString(),
                        targetPackage.asString()
                    )
                }
            }
        }
    }

    context(ctx: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSymbol(
        symbol: FirCallableSymbol<*>,
        callerPackage: FqName,
        expression: FirCall
    ) {
        val annotation = symbol.annotations.firstOrNull { it.hasPackagePrivateClassId() } ?: return

        val scopeOverride = extractScopeFromAnnotation(annotation)
        val callableId = symbol.callableId ?: return
        val targetPackage = if (scopeOverride.isNotEmpty()) {
            FqName(scopeOverride)
        } else {
            callableId.packageName
        }

        if (callerPackage != targetPackage) {
            val source = expression.source ?: return
            reporter.reportOn(
                source,
                PackagePrivateErrors.PACKAGE_PRIVATE_ACCESS,
                callableId.asSingleFqName().asString(),
                targetPackage.asString()
            )
        }
    }

    private fun extractScopeFromAnnotation(annotation: FirAnnotation): String {
        val argument = annotation.argumentMapping.mapping.entries.firstOrNull {
            it.key.asString() == "scope"
        }?.value
        return (argument as? FirLiteralExpression)?.value as? String ?: ""
    }
}

@OptIn(SymbolInternals::class)
private object RedundantPackagePrivateChecker : FirCallableDeclarationChecker(MppCheckerKind.Common) {
    context(ctx: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirCallableDeclaration) {
        // Check if this declaration has @PackagePrivate
        val hasPackagePrivate = declaration.annotations.any { it.hasPackagePrivateClassId() }
        if (!hasPackagePrivate) return
        
        // Check if containing class also has @PackagePrivate
        val dispatchType = declaration.dispatchReceiverType ?: return
        val containingClass = dispatchType.toRegularClassSymbol() ?: return
        val classHasPackagePrivate = containingClass.annotations.any { it.hasPackagePrivateClassId() }
        
        if (classHasPackagePrivate) {
            val source = declaration.source ?: return
            val callableId = declaration.symbol.callableId ?: return
            reporter.reportOn(
                source,
                PackagePrivateErrors.REDUNDANT_PACKAGE_PRIVATE,
                callableId.callableName.asString()
            )
        }
    }
}

@OptIn(SymbolInternals::class)
private object PackagePrivateTypeAliasChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    context(ctx: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        val fileSymbol = ctx.containingFileSymbol ?: return
        val callerPackage = fileSymbol.fir.packageDirective.packageFqName
        
        // Check if the type is a typealias
        val coneType = typeRef.coneType.abbreviatedTypeOrSelf
        @Suppress("DEPRECATION")
        val typeAliasSymbol = coneType.toTypeAliasSymbol(ctx.session) ?: return
        
        // Check if the typealias has @PackagePrivate
        val annotation = typeAliasSymbol.annotations.firstOrNull { it.hasPackagePrivateClassId() } ?: return
        
        val scopeOverride = extractScopeFromAnnotation(annotation)
        val targetPackage = if (scopeOverride.isNotEmpty()) {
            FqName(scopeOverride)
        } else {
            typeAliasSymbol.classId.packageFqName
        }
        
        if (callerPackage != targetPackage) {
            val source = typeRef.source ?: return
            reporter.reportOn(
                source,
                PackagePrivateErrors.PACKAGE_PRIVATE_ACCESS,
                typeAliasSymbol.classId.asSingleFqName().asString(),
                targetPackage.asString()
            )
        }
    }
    
    private fun extractScopeFromAnnotation(annotation: FirAnnotation): String {
        val argument = annotation.argumentMapping.mapping.entries.firstOrNull {
            it.key.asString() == "scope"
        }?.value
        return (argument as? FirLiteralExpression)?.value as? String ?: ""
    }
}
