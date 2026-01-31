import java.net.URLClassLoader

plugins { kotlin("jvm") }

kotlin { jvmToolchain(21) }

dependencies {
    implementation(project(":package-private-annotations"))
    kotlinCompilerPluginClasspath(project(":package-private-compiler-plugin"))
}

// For local development: run the analyzer via a custom task
// In standalone projects, use the gradle plugin: id("dev.packageprivate.package-private")
tasks.register("analyzePackagePrivateCandidates") {
    dependsOn(":package-private-gradle-plugin:jar")
    group = "verification"
    description = "Analyzes source code to find @PackagePrivate candidates"

    doLast {
        // Use the analyzer classes from the gradle plugin jar
        val pluginJar =
            project(":package-private-gradle-plugin")
                .tasks
                .named("jar")
                .get()
                .outputs
                .files
                .singleFile
        val kotlinCompilerJar =
            configurations
                .detachedConfiguration(
                    dependencies.create("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.3.0")
                )
                .resolve()

        val classLoader =
            URLClassLoader(
                (listOf(pluginJar) + kotlinCompilerJar).map { it.toURI().toURL() }.toTypedArray(),
                ClassLoader.getSystemClassLoader(),
            )

        val analyzerClass =
            classLoader.loadClass("dev.packageprivate.gradle.analyzer.SourceAnalyzer")
        val finderClass =
            classLoader.loadClass("dev.packageprivate.gradle.analyzer.CandidateFinder")

        val analyzer = analyzerClass.getDeclaredConstructor().newInstance()
        val sourceFiles =
            kotlin.sourceSets["main"].kotlin.srcDirs.flatMap { dir ->
                dir.walkTopDown().filter { it.extension == "kt" }.toList()
            }

        val result =
            analyzerClass.getMethod("analyze", List::class.java).invoke(analyzer, sourceFiles)
        val finder =
            finderClass
                .getDeclaredConstructor(Boolean::class.java, Boolean::class.java)
                .newInstance(true, true)
        val candidates =
            finderClass.getMethod("findCandidates", result.javaClass).invoke(finder, result)
                as List<*>

        analyzerClass.getMethod("dispose").invoke(analyzer)

        if (candidates.isEmpty()) {
            println("\nNo @PackagePrivate candidates found.\n")
        } else {
            println("\n Found ${candidates.size} @PackagePrivate candidates:\n")
            candidates.forEach { candidate ->
                println(candidate!!.javaClass.getMethod("format").invoke(candidate))
            }
        }
    }
}

// Run: ./gradlew :examples:example-gradle:analyzePackagePrivateCandidates
//
// The example source files in this module demonstrate:
// - InternalHelper.kt: Public class only used within package (CANDIDATE)
// - InternalObject.kt: Object only used within package (CANDIDATE)
// - utilityFunction: Internal function only used within package (CANDIDATE)
// - InternalService: Public class used across packages (NOT a candidate)
// - TypeUsageExample.kt: Demonstrates type reference detection
// - StarImportExample.kt: Demonstrates star import handling
// - QualifiedRefExample.kt: Demonstrates qualified reference handling
