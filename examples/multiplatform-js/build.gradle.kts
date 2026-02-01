plugins {
    kotlin("multiplatform") version "2.3.0"
    id("dev.packageprivate.package-private") version "1.2.0"
}

version = "1.2.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js {
        browser()
        nodejs()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
    }
}

// Note: The compiler plugin must be added explicitly for multiplatform projects
// The Gradle plugin's KotlinCompilerPluginSupportPlugin mechanism doesn't work reliably
// for non-JVM targets, so we use kotlinCompilerPluginClasspath directly.
dependencies {
    kotlinCompilerPluginClasspath("dev.packageprivate:package-private-compiler-plugin:1.2.0")
}

// The plugin automatically:
// - Adds the package-private-annotations dependency
// - Applies the compiler plugin for @PackagePrivate enforcement (via kotlinCompilerPluginClasspath above)
// - Registers the analyzePackagePrivateCandidates task
//
// Platform support:
// - JVM: Full compile-time + runtime enforcement via bytecode modification
// - JS/Native: Compile-time enforcement only via FIR checker
//
// The example source files in this module demonstrate:
// - Calculator.kt: Public class used within same package
// - InternalHelper.kt: Package-private helper used only in same package
// - Main.kt: Uses the public API from different package
// - PublicApi.kt: Contains commented violations - uncomment to see errors
