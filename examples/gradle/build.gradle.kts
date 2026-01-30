plugins {
  kotlin("jvm")
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  implementation(project(":package-private-annotations"))
  kotlinCompilerPluginClasspath(project(":package-private-compiler-plugin"))
}

// Note: To use the analyzer task, apply the package-private gradle plugin:
//   plugins {
//     id("dev.packageprivate.package-private") version "1.2.0"
//   }
// Then run: ./gradlew analyzePackagePrivateCandidates
