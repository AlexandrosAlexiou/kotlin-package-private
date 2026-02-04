package dev.packageprivate.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/** Configuration extension for the package-private plugin. */
open class PackagePrivateExtension {
    // No configuration needed - the plugin automatically adds the annotations dependency
    // and applies the compiler plugin
}

class PackagePrivateGradlePlugin : Plugin<Project> {
    companion object {
        private const val PLUGIN_VERSION = "1.3.0"
    }

    override fun apply(target: Project) {
        // Create extension
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
            
            // Add the compiler plugin to the Kotlin compiler classpath
            target.dependencies.add(
                "kotlinCompilerPluginClasspath",
                "dev.packageprivate:package-private-compiler-plugin:$PLUGIN_VERSION",
            )
        }
    }
}
