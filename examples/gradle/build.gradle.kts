plugins {
    kotlin("jvm") version "2.3.0"
    id("dev.packageprivate.package-private") version "1.2.0"
}

kotlin { jvmToolchain(21) }

// The plugin automatically:
// - Adds the package-private-annotations dependency
// - Applies the compiler plugin for @PackagePrivate enforcement
// - Registers the analyzePackagePrivateCandidates task
//
// Run: ./gradlew analyzePackagePrivateCandidates
//
// The example source files in this module demonstrate:
// - InternalHelper.kt: Public class only used within package (CANDIDATE)
// - InternalObject.kt: Object only used within package (CANDIDATE)
// - utilityFunction: Internal function only used within package (CANDIDATE)
// - InternalService: Public class used across packages (NOT a candidate)
// - TypeUsageExample.kt: Demonstrates type reference detection
// - StarImportExample.kt: Demonstrates star import handling
// - QualifiedRefExample.kt: Demonstrates qualified reference handling
