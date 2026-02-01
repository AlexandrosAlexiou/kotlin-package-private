rootProject.name = "package-private"

include(
  ":package-private-annotations",
  ":package-private-compiler-plugin",
  ":package-private-gradle-plugin",
  ":integration-tests",
  ":examples:java-gradle",
)

project(":examples:java-gradle").name = "example-java-gradle"

// example-gradle and example-multiplatform-js are standalone builds that use the plugin via includeBuild
// Run them with:
//   cd examples/gradle && ./gradlew analyzePackagePrivateCandidates
//   cd examples/multiplatform-js && ./gradlew analyzePackagePrivateCandidates
