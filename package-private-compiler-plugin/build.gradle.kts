plugins { kotlin("jvm") }

kotlin {
    jvmToolchain(21)
    compilerOptions { freeCompilerArgs.add("-Xcontext-parameters") }
}

val kotlinVersion = "2.3.10"

dependencies {
    compileOnly(kotlin("compiler-embeddable"))
    compileOnly("org.jetbrains.kotlin:kotlin-maven-plugin:$kotlinVersion")
    compileOnly("org.apache.maven:maven-project:2.2.1")
    compileOnly("org.apache.maven:maven-core:3.9.6")
    compileOnly("javax.inject:javax.inject:1")

    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
    testImplementation("dev.zacsweers.kctfork:core:0.12.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation(project(":package-private-annotations"))
}

tasks.test { useJUnitPlatform() }
