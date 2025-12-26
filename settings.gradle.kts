rootProject.name = "package-private"

include(
  ":package-private-annotations",
  ":package-private-compiler-plugin",
  ":package-private-gradle-plugin",
  ":integration-tests",
  ":examples:gradle",
  ":examples:java-gradle",
)

project(":examples:gradle").name = "example-gradle"
project(":examples:java-gradle").name = "example-java-gradle"
