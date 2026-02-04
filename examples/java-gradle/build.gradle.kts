plugins {
    kotlin("jvm") version "2.3.0"
    id("dev.packageprivate.package-private") version "1.3.0"
    id("dev.packageprivate.analyzer") version "1.3.0"
}

kotlin { jvmToolchain(21) }
