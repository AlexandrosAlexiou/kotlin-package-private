buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath("dev.packageprivate.analyzer:package-private-analyzer-gradle-plugin:1.3.0")
    }
}

plugins {
    kotlin("jvm") version "2.3.0"
}

apply(plugin = "dev.packageprivate.analyzer")

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("dev.packageprivate:package-private-annotations:1.3.0")
}
