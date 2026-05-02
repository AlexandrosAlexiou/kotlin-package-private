package dev.packageprivate.compiler

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error2
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.warning1
import org.jetbrains.kotlin.psi.KtElement

object PackagePrivateErrors : KtDiagnosticsContainer() {
    val PACKAGE_PRIVATE_ACCESS by
        error2<KtElement, String, String>(SourceElementPositioningStrategies.DEFAULT)

    val REDUNDANT_PACKAGE_PRIVATE by
        warning1<KtElement, String>(SourceElementPositioningStrategies.DEFAULT)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Renderers

    object Renderers : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by
            KtDiagnosticFactoryToRendererMap("PackagePrivate errors") {
                it.put(
                    PACKAGE_PRIVATE_ACCESS,
                    "Cannot access ''{0}'': it is package-private in ''{1}''",
                    TO_STRING,
                    TO_STRING,
                )
                it.put(
                    REDUNDANT_PACKAGE_PRIVATE,
                    "Redundant @PackagePrivate annotation: ''{0}'' is already package-private because its containing class is annotated with @PackagePrivate",
                    TO_STRING,
                )
            }
    }
}
