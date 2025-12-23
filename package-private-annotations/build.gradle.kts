plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    // JVM (includes Android)
    jvm()
    
    // Apple targets (requires Xcode)
    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()
    watchosX64()
    tvosArm64()
    tvosSimulatorArm64()
    tvosX64()
    
    // Linux targets
    linuxX64()
    linuxArm64()
    
    // Windows target
    mingwX64()
    
    // JS target (compile-time checks only, no runtime enforcement)
    js {
        browser()
        nodejs()
    }
    
    sourceSets {
        val commonMain by getting
    }
}
