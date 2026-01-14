rootProject.name = "package-private"

include(
  ":package-private-annotations",
  ":package-private-compiler-plugin",
  ":package-private-gradle-plugin",
  ":integration-tests",
  ":examples:gradle",
  ":examples:java-gradle",
  ":examples:multiplatform-js",
)

project(":examples:gradle").name = "example-gradle"
project(":examples:java-gradle").name = "example-java-gradle"
project(":examples:multiplatform-js").name = "example-multiplatform-js"
