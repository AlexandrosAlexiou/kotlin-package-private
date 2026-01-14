plugins {
  kotlin("multiplatform")
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
        implementation(project(":package-private-annotations"))
      }
    }
  }
}

dependencies {
  kotlinCompilerPluginClasspath(project(":package-private-compiler-plugin"))
}
