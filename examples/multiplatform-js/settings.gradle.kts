rootProject.name = "example-multiplatform-js"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
    
    // Include the parent build to resolve the plugin from source
    includeBuild("../..")
}
