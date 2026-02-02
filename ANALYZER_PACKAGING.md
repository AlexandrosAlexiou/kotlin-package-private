# Package Private Analyzer - Separate Packaging

The analyzer has been separated from the main package-private compiler plugin with its own versioning and packaging.

## Package Structure

### Main Package (Compiler Plugin)
- **Group ID:** `dev.packageprivate`
- **Version:** `1.2.0`
- **GitHub Repository:** `https://github.com/AlexandrosAlexiou/package-private`
- **GitHub Packages:** `https://maven.pkg.github.com/AlexandrosAlexiou/package-private`

**Modules:**
- `package-private-annotations` (multiplatform)
- `package-private-compiler-plugin`
- `package-private-gradle-plugin`

### Analyzer Package
- **Group ID:** `dev.packageprivate.analyzer`
- **Version:** `1.0.0`
- **GitHub Repository:** `https://github.com/AlexandrosAlexiou/package-private-analyzer`
- **GitHub Packages:** `https://maven.pkg.github.com/AlexandrosAlexiou/package-private-analyzer`

**Modules:**
- `package-private-analyzer-core`
- `package-private-analyzer-gradle-plugin`
- `package-private-analyzer-maven-plugin`

## Usage

### Gradle

```kotlin
plugins {
    // Compiler plugin for @PackagePrivate enforcement
    id("dev.packageprivate.package-private") version "1.2.0"
    
    // Analyzer plugin to find candidates
    id("dev.packageprivate.analyzer") version "1.0.0"
}
```

### Maven

```xml
<!-- Compiler Plugin -->
<plugin>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-maven-plugin</artifactId>
    <configuration>
        <compilerPlugins>
            <plugin>package-private</plugin>
        </compilerPlugins>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>dev.packageprivate</groupId>
            <artifactId>package-private-compiler-plugin</artifactId>
            <version>1.2.0</version>
        </dependency>
    </dependencies>
</plugin>

<!-- Analyzer Plugin -->
<plugin>
    <groupId>dev.packageprivate.analyzer</groupId>
    <artifactId>package-private-analyzer-maven-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <goals>
                <goal>analyze</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Versioning Strategy

The analyzer and compiler plugin can be versioned independently:

- **Compiler Plugin:** Updates when enforcement logic changes
- **Analyzer:** Updates when analysis logic changes

This allows:
- Stable compiler plugin versions while improving analysis
- Different release cycles for each tool
- Clearer separation of concerns
