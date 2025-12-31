package dev.packageprivate.compiler

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains
import kotlin.test.assertNotNull

@OptIn(ExperimentalCompilerApi::class)
class PackagePrivateCheckerTest {

    private val annotationSource = SourceFile.kotlin(
        "PackagePrivate.kt",
        """
        package dev.packageprivate

        @Target(
            AnnotationTarget.CLASS,
            AnnotationTarget.FUNCTION,
            AnnotationTarget.PROPERTY,
            AnnotationTarget.CONSTRUCTOR
        )
        @Retention(AnnotationRetention.BINARY)
        annotation class PackagePrivate(val scope: String = "")
        """
    )

    @Test
    fun `same package access is allowed`() {
        val result = compile(
            annotationSource,
            SourceFile.kotlin(
                "Internal.kt",
                """
                package com.example.internal

                import dev.packageprivate.PackagePrivate

                @PackagePrivate
                class Internal {
                    @PackagePrivate
                    fun secret(): String = "secret"
                }
                """
            ),
            SourceFile.kotlin(
                "SamePackage.kt",
                """
                package com.example.internal

                fun useSamePackage(): String = Internal().secret()
                """
            )
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `different package access to class is forbidden`() {
        val result = compile(
            annotationSource,
            SourceFile.kotlin(
                "Internal.kt",
                """
                package com.example.internal

                import dev.packageprivate.PackagePrivate

                @PackagePrivate
                class Internal
                """
            ),
            SourceFile.kotlin(
                "OtherPackage.kt",
                """
                package com.example.other

                import com.example.internal.Internal

                fun useOtherPackage() = Internal()
                """
            )
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "Cannot access 'com.example.internal.Internal'")
        assertContains(result.messages, "package-private in 'com.example.internal'")
    }

    @Test
    fun `different package access to function is forbidden`() {
        val result = compile(
            annotationSource,
            SourceFile.kotlin(
                "Internal.kt",
                """
                package com.example.internal

                import dev.packageprivate.PackagePrivate

                class Helper {
                    @PackagePrivate
                    fun secret(): String = "secret"
                }
                """
            ),
            SourceFile.kotlin(
                "OtherPackage.kt",
                """
                package com.example.other

                import com.example.internal.Helper

                fun useOtherPackage() = Helper().secret()
                """
            )
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "Cannot access")
        assertContains(result.messages, "package-private")
    }

    @Test
    fun `scope override allows access from specified package`() {
        val result = compile(
            annotationSource,
            SourceFile.kotlin(
                "Generated.kt",
                """
                package com.example.generated

                import dev.packageprivate.PackagePrivate

                @PackagePrivate(scope = "com.example.api")
                class GeneratedHelper {
                    fun help(): String = "help"
                }
                """
            ),
            SourceFile.kotlin(
                "Api.kt",
                """
                package com.example.api

                import com.example.generated.GeneratedHelper

                fun useGenerated() = GeneratedHelper().help()
                """
            )
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `scope override denies access from other packages`() {
        val result = compile(
            annotationSource,
            SourceFile.kotlin(
                "Generated.kt",
                """
                package com.example.generated

                import dev.packageprivate.PackagePrivate

                @PackagePrivate(scope = "com.example.api")
                class GeneratedHelper
                """
            ),
            SourceFile.kotlin(
                "Other.kt",
                """
                package com.example.other

                import com.example.generated.GeneratedHelper

                fun useGenerated() = GeneratedHelper()
                """
            )
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "package-private in 'com.example.api'")
    }

    @Test
    fun `package-private constructor is enforced`() {
        val result = compile(
            annotationSource,
            SourceFile.kotlin(
                "Internal.kt",
                """
                package com.example.internal

                import dev.packageprivate.PackagePrivate

                class Restricted @PackagePrivate constructor()
                """
            ),
            SourceFile.kotlin(
                "OtherPackage.kt",
                """
                package com.example.other

                import com.example.internal.Restricted

                fun create() = Restricted()
                """
            )
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "package-private")
    }

    @Test
    fun `top-level package-private function is enforced`() {
        val result = compile(
            annotationSource,
            SourceFile.kotlin(
                "Internal.kt",
                """
                package com.example.internal

                import dev.packageprivate.PackagePrivate

                @PackagePrivate
                fun internalUtil(): Int = 42
                """
            ),
            SourceFile.kotlin(
                "OtherPackage.kt",
                """
                package com.example.other

                import com.example.internal.internalUtil

                fun use() = internalUtil()
                """
            )
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "package-private")
    }

    @Test
    fun `warns on redundant @PackagePrivate on member of @PackagePrivate class`() {
        val result = compile(
            annotationSource,
            SourceFile.kotlin(
                "Internal.kt",
                """
                package com.example.internal

                import dev.packageprivate.PackagePrivate

                @PackagePrivate
                class KotlinInternal {
                    @PackagePrivate 
                    fun secret(): String = "secret"

                    fun publicMethod(): String = "public"
                }
                """
            )
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertContains(result.messages, "Redundant @PackagePrivate annotation")
        assertContains(result.messages, "'secret'")
    }

    @Test
    fun `no warning when @PackagePrivate on member of non-annotated class`() {
        val result = compile(
            annotationSource,
            SourceFile.kotlin(
                "Internal.kt",
                """
                package com.example.internal

                import dev.packageprivate.PackagePrivate

                class Helper {
                    @PackagePrivate 
                    fun secret(): String = "secret"
                }
                """
            )
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        // Should not contain redundant warning
        assertEquals(false, result.messages.contains("Redundant"), 
            "Should not warn about redundant annotation when class is not @PackagePrivate")
    }

    @Test
    fun `JVM bytecode sets package-private visibility on class`() {
        val result = compile(
            annotationSource,
            SourceFile.kotlin(
                "Internal.kt",
                """
                package com.example.internal

                import dev.packageprivate.PackagePrivate

                @PackagePrivate
                class HiddenClass {
                    fun work() = "work"
                }
                """
            )
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        
        // Verify the class is compiled and can be loaded
        val hiddenClass = result.classLoader.loadClass("com.example.internal.HiddenClass")
        assertEquals("com.example.internal.HiddenClass", hiddenClass.name)
        
        // On JVM, the class should be package-private (no public modifier)
        val isPublic = java.lang.reflect.Modifier.isPublic(hiddenClass.modifiers)
        assertEquals(false, isPublic, "Class should not be public (should be package-private)")
    }

    @Test
    fun `JVM bytecode sets package-private visibility on function`() {
        val result = compile(
            annotationSource,
            SourceFile.kotlin(
                "Helper.kt",
                """
                package com.example.internal

                import dev.packageprivate.PackagePrivate

                class Helper {
                    @PackagePrivate
                    fun secretMethod(): String = "secret"
                    
                    fun publicMethod(): String = "public"
                }
                """
            )
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        
        val helperClass = result.classLoader.loadClass("com.example.internal.Helper")
        
        // publicMethod should remain public
        val publicMethod = helperClass.getDeclaredMethod("publicMethod")
        assertEquals(true, java.lang.reflect.Modifier.isPublic(publicMethod.modifiers),
            "publicMethod() should remain public")
        
        // secretMethod should be package-private (not public)
        // Method might be name-mangled, so find it by prefix
        val secretMethod = helperClass.declaredMethods.find { it.name.startsWith("secretMethod") }
        assertNotNull(secretMethod, "secretMethod should exist")
        assertEquals(false, java.lang.reflect.Modifier.isPublic(secretMethod.modifiers),
            "secretMethod() should not be public (should be package-private)")
    }

    @Test
    fun `JVM bytecode sets package-private visibility on property`() {
        val result = compile(
            annotationSource,
            SourceFile.kotlin(
                "Data.kt",
                """
                package com.example.internal

                import dev.packageprivate.PackagePrivate

                class Data {
                    @PackagePrivate
                    val secretValue: String = "secret"
                    
                    val publicValue: String = "public"
                }
                """
            )
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        
        val dataClass = result.classLoader.loadClass("com.example.internal.Data")
        
        // publicValue getter should remain public
        val publicGetter = dataClass.getDeclaredMethod("getPublicValue")
        assertEquals(true, java.lang.reflect.Modifier.isPublic(publicGetter.modifiers),
            "getPublicValue() should remain public")
        
        // secretValue getter should be package-private (not public)
        // Getter might be name-mangled, so find it by prefix
        val secretGetter = dataClass.declaredMethods.find { it.name.contains("SecretValue") }
        assertNotNull(secretGetter, "secretValue getter should exist")
        assertEquals(false, java.lang.reflect.Modifier.isPublic(secretGetter.modifiers),
            "getSecretValue() should not be public (should be package-private)")
    }

    private fun compile(vararg sourceFiles: SourceFile): JvmCompilationResult {
        return KotlinCompilation().apply {
            sources = sourceFiles.toList()
            compilerPluginRegistrars = listOf(PackagePrivateComponentRegistrar())
            inheritClassPath = true
            messageOutputStream = System.out
            languageVersion = "2.0"
        }.compile()
    }
}
