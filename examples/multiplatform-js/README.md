# Kotlin Multiplatform JS example

This example demonstrates using the `@PackagePrivate` annotation in a Kotlin Multiplatform project targeting JavaScript.

## Analyzing Package-Private Candidates

```bash
cd examples/multiplatform-js
./gradlew analyzePackagePrivateCandidates
```

This will analyze all Kotlin source files (commonMain, jsMain, etc.) and suggest declarations that could benefit from `@PackagePrivate`.

## Building

```bash
# From this directory
./gradlew build

# Run the JS example
./gradlew jsBrowserDevelopmentRun
# or
./gradlew jsNodeDevelopmentRun
```

## Testing Package-Private Enforcement

Uncomment the lines in `PublicApi.kt`:

```kotlin
import com.example.utils.InternalHelper
fun broken() = InternalHelper()
```

Then build again:

```bash
./gradlew build
```

You'll see a compilation error:
```
Cannot access 'com.example.utils.InternalHelper': it is package-private in 'com.example.utils'
```

## Multiplatform Support

The gradle plugin's `analyzePackagePrivateCandidates` task **automatically discovers all Kotlin multiplatform source sets**:
- `commonMain`, `commonTest`
- `jvmMain`, `jsMain`, `nativeMain`
- `iosMain`, `androidMain`, `macosMain`, `linuxMain`, `mingwMain`
- `wasmJsMain`, `wasmWasiMain`
- `tvosMain`, `watchosMain`
- And any other configured targets

The SourceAnalyzer uses PSI parsing which works on any Kotlin syntax regardless of target platform. No configuration needed when adding new targets.

## Platform Notes

For **Kotlin/JS** and **Kotlin/Wasm**:
- Package-private enforcement is **compile-time only**
- JavaScript has no native visibility/access control concept
- The FIR checker prevents incorrect usage during compilation
- This is sufficient for most use cases - preventing accidental cross-package dependencies
