plugins {
    kotlin("multiplatform") version "2.3.0"
    id("dev.packageprivate.package-private")
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
        // Dependencies are automatically added by the plugin, but we can still declare them explicitly
      }
    }
  }
}

// The plugin automatically:
// - Adds the package-private-annotations dependency
// - Applies the compiler plugin for @PackagePrivate enforcement  
// - Registers the analyzePackagePrivateCandidates task
//
// Run: ./gradlew analyzePackagePrivateCandidates
//
// The example source files in this module demonstrate:
// - Calculator.kt: Public class used within same package
// - InternalHelper.kt: Package-private helper used only in same package
// - Main.kt: Uses the public API from different package
