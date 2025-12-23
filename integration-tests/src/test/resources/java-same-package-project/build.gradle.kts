plugins {
    kotlin("jvm") version "2.0.21"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("com.acme:package-private-annotations:0.1.0")
    kotlinCompilerPluginClasspath("com.acme:package-private-compiler-plugin:0.1.0")
}
