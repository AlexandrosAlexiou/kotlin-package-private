plugins {
  kotlin("jvm")
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.21")
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test {
  useJUnitPlatform()
  dependsOn(":package-private-annotations:publishToMavenLocal")
  dependsOn(":package-private-compiler-plugin:publishToMavenLocal")
}
