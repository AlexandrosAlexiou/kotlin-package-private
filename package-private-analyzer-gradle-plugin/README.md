# Package Private Analyzer Gradle Plugin

This Gradle plugin analyzes Kotlin code to find candidates for the `@PackagePrivate` annotation.

**Version:** 1.3.0  
**Group ID:** `dev.packageprivate.analyzer`  
**Plugin ID:** `dev.packageprivate.analyzer`

## Building

```bash
# From the project root
./gradlew :package-private-analyzer-gradle-plugin:build
./gradlew :package-private-analyzer-gradle-plugin:publishToMavenLocal
```

Or build all modules:

```bash
./gradlew build
./gradlew publishToMavenLocal
```

## Usage

Apply the plugin to your Gradle project:

### Using the plugins DSL

```kotlin
plugins {
    kotlin("jvm") version "2.3.0"
    id("dev.packageprivate.analyzer") version "1.3.0"
}
```

### Using legacy plugin application

```kotlin
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("dev.packageprivate.analyzer:package-private-analyzer-gradle-plugin:1.3.0")
    }
}

apply(plugin = "dev.packageprivate.analyzer")
```

## Running the Analyzer

The plugin registers a task called `analyzePackagePrivateCandidates`:

```bash
./gradlew analyzePackagePrivateCandidates
```

This task analyzes all Kotlin source files in your project and generates a report of declarations that could potentially use the `@PackagePrivate` annotation.

## Configuration

Configure the analyzer task in your `build.gradle.kts`:

```kotlin
import dev.packageprivate.analyzer.gradle.AnalyzePackagePrivateCandidatesTask

tasks.named<AnalyzePackagePrivateCandidatesTask>("analyzePackagePrivateCandidates") {
    includePublic.set(true)        // Analyze public declarations
    includeInternal.set(true)      // Analyze internal declarations
    outputFile.set(file("build/reports/package-private-candidates.txt"))
}
```

### Available Configuration Options

- **`includePublic`** (default: `true`) - Include public declarations in candidate analysis
- **`includeInternal`** (default: `true`) - Include internal declarations in candidate analysis
- **`outputFile`** (default: `build/reports/package-private-candidates.txt`) - Output file path for the analysis report

## Output

The analyzer generates a text report listing all declarations that are:
- Currently `public` or `internal`
- Only used within their declaring package
- Potential candidates for the `@PackagePrivate` annotation

Example output:

```
Package Private Candidates Report
==================================

Found 3 candidates for @PackagePrivate annotation:

1. [CLASS] com.example.InternalHelper (line 5)
   - Currently: public
   - Used only in: com.example
   - File: src/main/kotlin/com/example/InternalHelper.kt

2. [OBJECT] com.example.InternalObject (line 3)
   - Currently: public
   - Used only in: com.example
   - File: src/main/kotlin/com/example/InternalObject.kt

3. [FUNCTION] com.example.utilityFunction (line 7)
   - Currently: internal
   - Used only in: com.example
   - File: src/main/kotlin/com/example/Utils.kt
```

## Integration with Compiler Plugin

This analyzer plugin works alongside the `dev.packageprivate.package-private` compiler plugin:

1. **Analyzer Plugin** - Finds candidates for `@PackagePrivate` (this plugin)
2. **Compiler Plugin** - Enforces `@PackagePrivate` restrictions at compile time

Typical workflow:

```kotlin
plugins {
    kotlin("jvm") version "2.3.0"
    id("dev.packageprivate.package-private") version "1.3.0"  // Compiler plugin
    id("dev.packageprivate.analyzer") version "1.3.0"         // Analyzer plugin
}
```

```bash
# 1. Analyze your code to find candidates
./gradlew analyzePackagePrivateCandidates

# 2. Review the report and add @PackagePrivate annotations

# 3. Build with enforcement
./gradlew build
```

## Example

See the [examples/gradle](../../examples/gradle) directory for a complete working example.
