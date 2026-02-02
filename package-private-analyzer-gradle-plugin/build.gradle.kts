plugins {
  kotlin("jvm")
  id("java-gradle-plugin")
  id("maven-publish")
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  implementation(project(":package-private-analyzer-core"))
  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.0")
  
  testImplementation("org.jetbrains.kotlin:kotlin-test:2.3.0")
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test { useJUnitPlatform() }

gradlePlugin {
  plugins {
    create("packagePrivateAnalyzer") {
      id = "dev.packageprivate.analyzer"
      implementationClass = "dev.packageprivate.analyzer.gradle.PackagePrivateAnalyzerGradlePlugin"
      displayName = "Package Private Analyzer"
      description = "Analyzes Kotlin code to find candidates for @PackagePrivate annotation"
    }
  }
}

publishing {
  repositories {
    mavenLocal()
  }
}
