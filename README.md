# kotlin-package-private

A Kotlin compiler plugin that adds **package-private** visibility to Kotlin, similar to Java's default (package) visibility.

## Features

- `@PackagePrivate` annotation for classes, functions, properties, and constructors
- Compile-time enforcement - access violations are reported as compilation errors
- Optional `scope` parameter to override the package (useful for generated code)
- Works with both **Gradle** and **Maven**
- Supports Kotlin 2.0+ (K2 compiler)

## Installation

### Repository Setup

Add the GitHub Packages repository (requires authentication):

<details>
<summary><b>Gradle Setup</b></summary>

Add to `~/.gradle/gradle.properties`:
```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

Create a token at https://github.com/settings/tokens with `read:packages` scope.
</details>

<details>
<summary><b>Maven Setup</b></summary>

Add to `~/.m2/settings.xml`:
```xml
<servers>
  <server>
    <id>github</id>
    <username>YOUR_GITHUB_USERNAME</username>
    <password>YOUR_GITHUB_TOKEN</password>
  </server>
</servers>
```
</details>

### Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.0.21"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/AlexandrosAlexiou/kotlin-package-private")
        credentials {
            username = project.findProperty("gpr.user") as String?
            password = project.findProperty("gpr.key") as String?
        }
    }
}

dependencies {
    implementation("com.acme:package-private-annotations:0.1.0")
    kotlinCompilerPluginClasspath("com.acme:package-private-compiler-plugin:0.1.0")
}
```

### Gradle (Groovy DSL)

```groovy
// build.gradle
plugins {
    id 'org.jetbrains.kotlin.jvm' version '2.0.21'
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/AlexandrosAlexiou/kotlin-package-private")
        credentials {
            username = project.findProperty("gpr.user")
            password = project.findProperty("gpr.key")
        }
    }
}

dependencies {
    implementation 'com.acme:package-private-annotations:0.1.0'
    kotlinCompilerPluginClasspath 'com.acme:package-private-compiler-plugin:0.1.0'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/AlexandrosAlexiou/kotlin-package-private</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.acme</groupId>
        <artifactId>package-private-annotations</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-maven-plugin</artifactId>
            <version>2.0.21</version>
            <executions>
                <execution>
                    <id>compile</id>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <compilerPlugins>
                    <plugin>package-private</plugin>
                </compilerPlugins>
            </configuration>
            <dependencies>
                <dependency>
                    <groupId>com.acme</groupId>
                    <artifactId>package-private-compiler-plugin</artifactId>
                    <version>0.1.0</version>
                </dependency>
            </dependencies>
        </plugin>
    </plugins>
</build>
```

## Usage

### Basic Usage

```kotlin
// com/example/internal/Helper.kt
package com.example.internal

import com.acme.packageprivate.PackagePrivate

@PackagePrivate
class Helper {
    @PackagePrivate
    fun doSomething(): String = "internal work"
}

@PackagePrivate
fun utilityFunction(): Int = 42
```

```kotlin
// com/example/internal/Service.kt
package com.example.internal

// ✅ Same package - allowed
class Service {
    fun execute() = Helper().doSomething()
}
```

```kotlin
// com/example/api/PublicApi.kt
package com.example.api

import com.example.internal.Helper

// ❌ Compilation error:
// Cannot access 'com.example.internal.Helper': it is package-private in 'com.example.internal'
fun broken() = Helper()
```

### Annotation Targets

`@PackagePrivate` can be applied to:

| Target | Example |
|--------|---------|
| Class | `@PackagePrivate class Internal` |
| Function | `@PackagePrivate fun helper()` |
| Property | `@PackagePrivate val config: Config` |
| Constructor | `class Foo @PackagePrivate constructor()` |

### Scope Override

The `scope` parameter allows overriding the package scope. This is useful for:
- Generated code that ends up in a different package
- Refactored code where the original package should still have access

```kotlin
package com.example.generated

import com.acme.packageprivate.PackagePrivate

// Accessible from com.example.api instead of com.example.generated
@PackagePrivate(scope = "com.example.api")
class GeneratedHelper
```

## How It Works

This project uses a **Kotlin Compiler Plugin** (not KSP) because:

1. **KSP** is designed for code generation and symbol processing - it cannot report compilation errors for cross-file access violations
2. **Compiler plugins** can intercept the compilation pipeline and report errors during the FIR (Frontend IR) analysis phase

The plugin registers a `FirAdditionalCheckersExtension` that inspects all call expressions and verifies that package-private symbols are only accessed from within their declared (or overridden) package.

## Project Structure

```
kotlin-package-private/
├── package-private-annotations/     # @PackagePrivate annotation
├── package-private-compiler-plugin/ # K2 FIR compiler plugin (works with both Gradle & Maven)
├── package-private-gradle-plugin/   # Gradle plugin (optional, for cleaner DSL)
└── examples/
    ├── gradle/                      # Gradle example project
    └── maven/                       # Maven example project
```

## Why No Separate Maven Plugin Module?

- **Gradle** requires a custom plugin (`KotlinCompilerPluginSupportPlugin`) to integrate compiler plugins
- **Maven** has built-in support via `kotlin-maven-plugin` - the compiler plugin JAR includes a Plexus `components.xml` that registers the extension automatically
- Both use the same `package-private-compiler-plugin` artifact

## Requirements

- Kotlin 2.0.21 or later (K2 compiler)
- JDK 21 or later

## Building from Source

```bash
./gradlew build
```

To test the Gradle example with a violation:

```bash
# Edit examples/gradle/src/main/kotlin/com/acme/bar/TryUseOtherPackage.kt
# Uncomment the import and usage of Hidden class
./gradlew :examples:example-gradle:build
# Should fail with: Cannot access 'com.acme.foo.Hidden': it is package-private in 'com.acme.foo'
```

To test the Maven example (requires publishing to local Maven repo first):

```bash
# Publish to local Maven repository
./gradlew publishToMavenLocal

# Build Maven example
cd examples/maven
mvn compile
# Should succeed

# Edit src/main/kotlin/com/acme/bar/TryUseOtherPackage.kt
# Uncomment the import and usage of Hidden class
mvn compile
# Should fail with: Cannot access 'com.acme.foo.Hidden': it is package-private in 'com.acme.foo'
```

## Publishing

### To Local Maven Repository

```bash
./gradlew publishToMavenLocal
```

## License

Apache License 2.0
