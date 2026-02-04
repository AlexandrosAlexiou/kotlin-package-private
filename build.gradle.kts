plugins {
  kotlin("jvm") version "2.3.0" apply false
  kotlin("multiplatform") version "2.3.0" apply false
}

// Unified version for all components
group = "dev.packageprivate"
version = "1.3.0"

repositories {
  mavenCentral()
}

subprojects {
  // Set group based on project type
  val isAnalyzer = project.name.contains("analyzer")
  group = if (isAnalyzer) "dev.packageprivate.analyzer" else rootProject.group
  version = rootProject.version

  repositories {
    mavenCentral()
  }

  // Apply maven-publish to library modules (not examples, integration-tests, or gradle-plugin)
  if (!project.path.startsWith(":examples") && 
      project.name != "integration-tests" && 
      project.name != "package-private-gradle-plugin" &&
      project.name != "package-private-analyzer-gradle-plugin") {
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
              groupId = project.group.toString()
              artifactId = project.name
              version = project.version.toString()

              pom {
                name.set(project.name)
                description.set(
                  if (isAnalyzer) "Analyzer for package-private candidates in Kotlin code"
                  else "Kotlin package-private visibility compiler plugin"
                )
                url.set("https://github.com/AlexandrosAlexiou/package-private")

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
                  url.set("https://github.com/AlexandrosAlexiou/package-private")
                  connection.set("scm:git:git://github.com/AlexandrosAlexiou/package-private.git")
                  developerConnection.set("scm:git:ssh://github.com/AlexandrosAlexiou/package-private.git")
                }
              }
            }
          }
        }

        repositories {
          // GitHub Packages - same repo, different group IDs create separate packages
          maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/AlexandrosAlexiou/package-private")
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
