plugins {
    kotlin("jvm") version "2.3.21"
    id("dev.packageprivate.package-private") version "1.3.2"
    id("dev.packageprivate.analyzer") version "1.3.2"
}

kotlin { jvmToolchain(21) }
