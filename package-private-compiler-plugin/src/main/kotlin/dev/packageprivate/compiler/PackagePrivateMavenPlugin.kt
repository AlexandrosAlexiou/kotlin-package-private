package dev.packageprivate.compiler

import org.apache.maven.plugin.MojoExecution
import org.apache.maven.project.MavenProject
import org.jetbrains.kotlin.maven.KotlinMavenPluginExtension
import org.jetbrains.kotlin.maven.PluginOption
import javax.inject.Named

/**
 * Maven plugin extension that registers the package-private compiler plugin.
 * The @Named annotation specifies the plugin name used in pom.xml configuration.
 */
@Named("package-private")
class PackagePrivateMavenPlugin : KotlinMavenPluginExtension {
    override fun getCompilerPluginId(): String = "dev.packageprivate.package-private"

    override fun isApplicable(project: MavenProject, execution: MojoExecution): Boolean = true

    override fun getPluginOptions(project: MavenProject, execution: MojoExecution): List<PluginOption> = emptyList()
}
