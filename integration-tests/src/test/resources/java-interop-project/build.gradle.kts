plugins {
    kotlin("jvm") version "2.3.0"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("dev.packageprivate:package-private-annotations:1.1.0")
    kotlinCompilerPluginClasspath("dev.packageprivate:package-private-compiler-plugin:1.1.0")
}
