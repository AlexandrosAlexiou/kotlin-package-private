plugins {
  kotlin("jvm") version "2.0.21" apply false
  kotlin("multiplatform") version "2.0.21" apply false
}

group = "com.acme"
version = "0.1.0"

repositories {
  mavenCentral()
}

subprojects {
  group = rootProject.group
  version = rootProject.version

  repositories {
    mavenCentral()
  }

  // Apply maven-publish to library modules (not examples, integration-tests, or gradle-plugin)
  if (!project.path.startsWith(":examples") && 
      project.name != "integration-tests" && 
      project.name != "package-private-gradle-plugin") {
    apply(plugin = "maven-publish")

    afterEvaluate {
      configure<PublishingExtension> {
        // For multiplatform projects, Kotlin automatically creates publications
        // For JVM-only projects, create a maven publication manually
        val isMultiplatform = plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
        
        if (!isMultiplatform && components.findByName("kotlin") != null) {
          publications {
            create<MavenPublication>("maven") {
              from(components["kotlin"])
              groupId = rootProject.group.toString()
              artifactId = project.name
              version = rootProject.version.toString()

              pom {
                name.set(project.name)
                description.set("Kotlin package-private visibility compiler plugin")
                url.set("https://github.com/AlexandrosAlexiou/kotlin-package-private")

                licenses {
                  license {
                    name.set("Apache License 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0")
                  }
                }

                developers {
                  developer {
                    id.set("AlexandrosAlexiou")
                    name.set("Alexandros Alexiou")
                  }
                }

                scm {
                  url.set("https://github.com/AlexandrosAlexiou/kotlin-package-private")
                  connection.set("scm:git:git://github.com/AlexandrosAlexiou/kotlin-package-private.git")
                  developerConnection.set("scm:git:ssh://github.com/AlexandrosAlexiou/kotlin-package-private.git")
                }
              }
            }
          }
        }

        repositories {
          // GitHub Packages
          maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/AlexandrosAlexiou/kotlin-package-private")
            credentials {
              username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
              password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
          }

          // Local Maven repository (for testing)
          mavenLocal()
        }
      }
    }
  }
}
