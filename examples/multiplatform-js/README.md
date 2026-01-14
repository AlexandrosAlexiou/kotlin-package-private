# Kotlin Multiplatform JS example

This example demonstrates using the `@PackagePrivate` annotation in a Kotlin Multiplatform project targeting JavaScript.

## Building

```bash
# From the repository root
./gradlew :examples:multiplatform-js:build

# Run the JS example
./gradlew :examples:multiplatform-js:jsBrowserDevelopmentRun
# or
./gradlew :examples:multiplatform-js:jsNodeDevelopmentRun
```

## Testing Package-Private Enforcement

Uncomment the lines in `PublicApi.kt`:

```kotlin
import com.example.utils.InternalHelper
fun broken() = InternalHelper()
```

Then build again:

```bash
./gradlew :examples:multiplatform-js:build
```

You'll see a compilation error:
```
Cannot access 'com.example.utils.InternalHelper': it is package-private in 'com.example.utils'
```

## Platform Notes

For **Kotlin/JS** and **Kotlin/Wasm**:
- Package-private enforcement is **compile-time only**
- JavaScript has no native visibility/access control concept
- The FIR checker prevents incorrect usage during compilation
- This is sufficient for most use cases - preventing accidental cross-package dependencies
