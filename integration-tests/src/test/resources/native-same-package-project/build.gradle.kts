plugins {
    kotlin("multiplatform") version "2.0.21"
}

repositories {
    mavenLocal()
    mavenCentral()
}

val compilerPlugin by configurations.creating

dependencies {
    compilerPlugin("com.acme:package-private-compiler-plugin:0.1.0")
}

kotlin {
    // Use the host target for native compilation
    val hostOs = System.getProperty("os.name")
    val hostArch = System.getProperty("os.arch")
    val nativeTarget = when {
        hostOs == "Mac OS X" && hostArch == "aarch64" -> macosArm64("native")
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" && hostArch == "aarch64" -> linuxArm64("native")
        hostOs == "Linux" -> linuxX64("native")
        hostOs.startsWith("Windows") -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "com.example.internal.main"
            }
        }
    }

    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation("com.acme:package-private-annotations:0.1.0")
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xplugin=${compilerPlugin.files.first()}")
    }
}
