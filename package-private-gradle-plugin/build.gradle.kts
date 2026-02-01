plugins {
  kotlin("jvm")
  id("java-gradle-plugin")
}

kotlin {
  jvmToolchain(21)
  compilerOptions {
    // Opt-in to K1 API until K2-compatible standalone PSI parsing is available
    freeCompilerArgs.add("-opt-in=org.jetbrains.kotlin.K1Deprecation")
  }
}

dependencies {
  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.0")
  // For Kotlin PSI parsing in the analyzer
  implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.3.0")
  
  testImplementation("org.jetbrains.kotlin:kotlin-test:2.3.0")
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test { useJUnitPlatform() }

gradlePlugin {
  plugins {
    create("packagePrivate") {
      id = "dev.packageprivate.package-private"
      implementationClass = "dev.packageprivate.gradle.PackagePrivateGradlePlugin"
    }
  }
}
