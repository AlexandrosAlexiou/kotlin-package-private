plugins {
  kotlin("jvm")
  id("java-gradle-plugin")
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.3.0")
}

gradlePlugin {
  plugins {
    create("packagePrivate") {
      id = "dev.packageprivate.package-private"
      implementationClass = "dev.packageprivate.gradle.PackagePrivateGradlePlugin"
    }
  }
}
