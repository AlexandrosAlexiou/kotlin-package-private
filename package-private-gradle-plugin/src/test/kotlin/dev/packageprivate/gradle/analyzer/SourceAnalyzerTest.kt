package dev.packageprivate.gradle.analyzer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SourceAnalyzerTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `finds public class only used in same package as candidate`() {
        // Create source files
        val internalPkg = File(tempDir, "com/example/internal").apply { mkdirs() }
        
        File(internalPkg, "Helper.kt").writeText("""
            package com.example.internal
            
            class Helper {
                fun doWork(): String = "work"
            }
        """.trimIndent())
        
        File(internalPkg, "Service.kt").writeText("""
            package com.example.internal
            
            class Service {
                fun execute() = Helper().doWork()
            }
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            // Helper should be a candidate (only used in same package)
            val helperCandidate = candidates.find { it.declaration.name == "Helper" }
            assertTrue(helperCandidate != null, "Helper should be a candidate")
            assertEquals("com.example.internal", helperCandidate.declaration.packageName)
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `excludes class used from different package`() {
        // Create source files
        val internalPkg = File(tempDir, "com/example/internal").apply { mkdirs() }
        val apiPkg = File(tempDir, "com/example/api").apply { mkdirs() }
        
        File(internalPkg, "Helper.kt").writeText("""
            package com.example.internal
            
            class Helper {
                fun doWork(): String = "work"
            }
        """.trimIndent())
        
        File(apiPkg, "Api.kt").writeText("""
            package com.example.api
            
            import com.example.internal.Helper
            
            class Api {
                fun call() = Helper().doWork()
            }
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            // Helper should NOT be a candidate (used from different package)
            val helperCandidate = candidates.find { it.declaration.name == "Helper" }
            assertTrue(helperCandidate == null, "Helper should NOT be a candidate when used cross-package")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `excludes already annotated declarations`() {
        val internalPkg = File(tempDir, "com/example/internal").apply { mkdirs() }
        
        File(internalPkg, "Helper.kt").writeText("""
            package com.example.internal
            
            @PackagePrivate
            class Helper {
                fun doWork(): String = "work"
            }
        """.trimIndent())
        
        File(internalPkg, "Service.kt").writeText("""
            package com.example.internal
            
            class Service {
                fun execute() = Helper().doWork()
            }
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            // Helper should NOT be a candidate (already annotated)
            val helperCandidate = candidates.find { it.declaration.name == "Helper" }
            assertTrue(helperCandidate == null, "Helper should NOT be a candidate when already annotated")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `excludes private declarations`() {
        val internalPkg = File(tempDir, "com/example/internal").apply { mkdirs() }
        
        File(internalPkg, "Helper.kt").writeText("""
            package com.example.internal
            
            private class Helper {
                fun doWork(): String = "work"
            }
            
            fun useHelper() = Helper().doWork()
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            // Helper should NOT be a candidate (already private)
            val helperCandidate = candidates.find { it.declaration.name == "Helper" }
            assertTrue(helperCandidate == null, "Private declarations should not be candidates")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `finds internal function only used in same package as candidate`() {
        val internalPkg = File(tempDir, "com/example/internal").apply { mkdirs() }
        
        File(internalPkg, "Utils.kt").writeText("""
            package com.example.internal
            
            internal fun helperFunction(): Int = 42
        """.trimIndent())
        
        File(internalPkg, "Service.kt").writeText("""
            package com.example.internal
            
            fun useHelper() = helperFunction()
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder(includeInternal = true)
            val candidates = finder.findCandidates(result)
            
            // helperFunction should be a candidate
            val funcCandidate = candidates.find { it.declaration.name == "helperFunction" }
            assertTrue(funcCandidate != null, "Internal function only used in same package should be a candidate")
            assertEquals(Visibility.INTERNAL, funcCandidate.declaration.visibility)
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `respects includePublic configuration`() {
        val internalPkg = File(tempDir, "com/example/internal").apply { mkdirs() }
        
        File(internalPkg, "Helper.kt").writeText("""
            package com.example.internal
            
            class PublicHelper
            internal class InternalHelper
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            // With includePublic = false
            val finderNoPublic = CandidateFinder(includePublic = false, includeInternal = true)
            val candidatesNoPublic = finderNoPublic.findCandidates(result)
            
            assertTrue(candidatesNoPublic.none { it.declaration.name == "PublicHelper" }, 
                "Public declarations should be excluded when includePublic = false")
            assertTrue(candidatesNoPublic.any { it.declaration.name == "InternalHelper" },
                "Internal declarations should still be included")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `finds property only used in same package as candidate`() {
        val internalPkg = File(tempDir, "com/example/internal").apply { mkdirs() }
        
        File(internalPkg, "Config.kt").writeText("""
            package com.example.internal
            
            val internalConfig: String = "config value"
        """.trimIndent())
        
        File(internalPkg, "Service.kt").writeText("""
            package com.example.internal
            
            fun useConfig() = internalConfig
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            val propCandidate = candidates.find { it.declaration.name == "internalConfig" }
            assertTrue(propCandidate != null, "Property only used in same package should be a candidate")
            assertEquals(DeclarationKind.PROPERTY, propCandidate.declaration.kind)
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `handles multiple packages correctly`() {
        val pkg1 = File(tempDir, "com/example/pkg1").apply { mkdirs() }
        val pkg2 = File(tempDir, "com/example/pkg2").apply { mkdirs() }
        val pkg3 = File(tempDir, "com/example/pkg3").apply { mkdirs() }
        
        // Helper1 - only used in pkg1 (candidate)
        File(pkg1, "Helper1.kt").writeText("""
            package com.example.pkg1
            class Helper1
        """.trimIndent())
        
        File(pkg1, "User1.kt").writeText("""
            package com.example.pkg1
            fun use1() = Helper1()
        """.trimIndent())
        
        // Helper2 - used in pkg2 and pkg3 (NOT a candidate)
        File(pkg2, "Helper2.kt").writeText("""
            package com.example.pkg2
            class Helper2
        """.trimIndent())
        
        File(pkg2, "User2.kt").writeText("""
            package com.example.pkg2
            fun use2() = Helper2()
        """.trimIndent())
        
        File(pkg3, "User3.kt").writeText("""
            package com.example.pkg3
            import com.example.pkg2.Helper2
            fun use3() = Helper2()
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            assertTrue(candidates.any { it.declaration.name == "Helper1" }, 
                "Helper1 should be a candidate (only used in pkg1)")
            assertTrue(candidates.none { it.declaration.name == "Helper2" }, 
                "Helper2 should NOT be a candidate (used cross-package)")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `finds class method only used in same package`() {
        val internalPkg = File(tempDir, "com/example/internal").apply { mkdirs() }
        
        File(internalPkg, "Helper.kt").writeText("""
            package com.example.internal
            
            class Helper {
                fun internalMethod(): Int = 42
            }
        """.trimIndent())
        
        File(internalPkg, "Service.kt").writeText("""
            package com.example.internal
            
            fun useHelper() = Helper().internalMethod()
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            val methodCandidate = candidates.find { it.declaration.name == "internalMethod" }
            assertTrue(methodCandidate != null, "Method only used in same package should be a candidate")
            assertEquals(DeclarationKind.FUNCTION, methodCandidate.declaration.kind)
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `respects includeInternal configuration`() {
        val internalPkg = File(tempDir, "com/example/internal").apply { mkdirs() }
        
        File(internalPkg, "Helper.kt").writeText("""
            package com.example.internal
            
            class PublicHelper
            internal class InternalHelper
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            // With includeInternal = false
            val finderNoInternal = CandidateFinder(includePublic = true, includeInternal = false)
            val candidatesNoInternal = finderNoInternal.findCandidates(result)
            
            assertTrue(candidatesNoInternal.any { it.declaration.name == "PublicHelper" },
                "Public declarations should still be included")
            assertTrue(candidatesNoInternal.none { it.declaration.name == "InternalHelper" }, 
                "Internal declarations should be excluded when includeInternal = false")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `excludes protected declarations`() {
        val internalPkg = File(tempDir, "com/example/internal").apply { mkdirs() }
        
        File(internalPkg, "Base.kt").writeText("""
            package com.example.internal
            
            open class Base {
                protected fun protectedMethod(): Int = 42
            }
        """.trimIndent())
        
        File(internalPkg, "Child.kt").writeText("""
            package com.example.internal
            
            class Child : Base() {
                fun useProtected() = protectedMethod()
            }
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            val protectedCandidate = candidates.find { it.declaration.name == "protectedMethod" }
            assertTrue(protectedCandidate == null, "Protected declarations should not be candidates")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `candidate format includes all required information`() {
        val internalPkg = File(tempDir, "com/example/internal").apply { mkdirs() }
        
        File(internalPkg, "Helper.kt").writeText("""
            package com.example.internal
            
            class Helper
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            val candidate = candidates.find { it.declaration.name == "Helper" }
            assertTrue(candidate != null)
            
            val formatted = candidate.format()
            assertTrue(formatted.contains("com.example.internal.Helper"), "Should contain FQ name")
            assertTrue(formatted.contains("class"), "Should contain kind")
            assertTrue(formatted.contains("public"), "Should contain visibility")
            assertTrue(formatted.contains("Helper.kt"), "Should contain file name")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `handles star imports correctly`() {
        val internalPkg = File(tempDir, "com/example/internal").apply { mkdirs() }
        val apiPkg = File(tempDir, "com/example/api").apply { mkdirs() }
        
        File(internalPkg, "Helper.kt").writeText("""
            package com.example.internal
            
            class Helper
        """.trimIndent())
        
        File(apiPkg, "Api.kt").writeText("""
            package com.example.api
            
            import com.example.internal.*
            
            fun useHelper() = Helper()
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            // Helper should NOT be a candidate (used via star import from different package)
            val helperCandidate = candidates.find { it.declaration.name == "Helper" }
            assertTrue(helperCandidate == null, "Helper should NOT be a candidate when used via star import cross-package")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `handles qualified references correctly`() {
        val internalPkg = File(tempDir, "com/example/internal").apply { mkdirs() }
        val apiPkg = File(tempDir, "com/example/api").apply { mkdirs() }
        
        File(internalPkg, "Helper.kt").writeText("""
            package com.example.internal
            
            class Helper
        """.trimIndent())
        
        File(apiPkg, "Api.kt").writeText("""
            package com.example.api
            
            fun useHelper() = com.example.internal.Helper()
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            // Helper should NOT be a candidate (used via qualified reference from different package)
            val helperCandidate = candidates.find { it.declaration.name == "Helper" }
            assertTrue(helperCandidate == null, "Helper should NOT be a candidate when used via qualified reference cross-package")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `handles type references correctly`() {
        val internalPkg = File(tempDir, "com/example/internal").apply { mkdirs() }
        val apiPkg = File(tempDir, "com/example/api").apply { mkdirs() }
        
        File(internalPkg, "Helper.kt").writeText("""
            package com.example.internal
            
            class Helper
        """.trimIndent())
        
        File(apiPkg, "Api.kt").writeText("""
            package com.example.api
            
            import com.example.internal.Helper
            
            fun useHelper(): Helper? = null
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            // Helper should NOT be a candidate (used as type reference from different package)
            val helperCandidate = candidates.find { it.declaration.name == "Helper" }
            assertTrue(helperCandidate == null, "Helper should NOT be a candidate when used as type reference cross-package")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `finds object declaration as candidate`() {
        val internalPkg = File(tempDir, "com/example/internal").apply { mkdirs() }
        
        File(internalPkg, "Singleton.kt").writeText("""
            package com.example.internal
            
            object Singleton {
                fun doWork(): Int = 42
            }
        """.trimIndent())
        
        File(internalPkg, "Service.kt").writeText("""
            package com.example.internal
            
            fun useSingleton() = Singleton.doWork()
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            val singletonCandidate = candidates.find { it.declaration.name == "Singleton" }
            assertTrue(singletonCandidate != null, "Object declaration should be a candidate")
            assertEquals(DeclarationKind.OBJECT, singletonCandidate.declaration.kind)
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `excludes object used cross-package`() {
        val internalPkg = File(tempDir, "com/example/internal").apply { mkdirs() }
        val apiPkg = File(tempDir, "com/example/api").apply { mkdirs() }
        
        File(internalPkg, "Singleton.kt").writeText("""
            package com.example.internal
            
            object Singleton {
                fun doWork(): Int = 42
            }
        """.trimIndent())
        
        File(apiPkg, "Api.kt").writeText("""
            package com.example.api
            
            import com.example.internal.Singleton
            
            fun useSingleton() = Singleton.doWork()
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            val singletonCandidate = candidates.find { it.declaration.name == "Singleton" }
            assertTrue(singletonCandidate == null, "Object used cross-package should NOT be a candidate")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `comprehensive example with all features`() {
        // Simulates the example-gradle module structure
        val internalPkg = File(tempDir, "com/acme/internal").apply { mkdirs() }
        val apiPkg = File(tempDir, "com/acme/api").apply { mkdirs() }
        val otherPkg = File(tempDir, "com/acme/other").apply { mkdirs() }
        
        // InternalHelper - should be a candidate (only used within package)
        File(internalPkg, "InternalHelper.kt").writeText("""
            package com.acme.internal
            
            class InternalHelper {
                fun compute(): Int = 42
            }
            
            internal fun utilityFunction(): String = "utility"
        """.trimIndent())
        
        // InternalObject - should be a candidate (only used within package)
        File(internalPkg, "InternalObject.kt").writeText("""
            package com.acme.internal
            
            object InternalObject {
                fun getInstance(): String = "singleton"
            }
        """.trimIndent())
        
        // InternalService - uses InternalHelper, utilityFunction, InternalObject within same package
        File(internalPkg, "InternalService.kt").writeText("""
            package com.acme.internal
            
            class InternalService {
                private val helper = InternalHelper()
                
                fun doWork(): String {
                    val result = helper.compute()
                    val util = utilityFunction()
                    val singleton = InternalObject.getInstance()
                    return "Result: ${'$'}result"
                }
            }
        """.trimIndent())
        
        // TypeUsageExample - uses type references
        File(internalPkg, "TypeUsageExample.kt").writeText("""
            package com.acme.internal
            
            class TypeUsageExample {
                private val helper: InternalHelper = InternalHelper()
                fun process(h: InternalHelper): Int = h.compute()
            }
        """.trimIndent())
        
        // PublicApi - uses InternalService from different package
        File(apiPkg, "PublicApi.kt").writeText("""
            package com.acme.api
            
            import com.acme.internal.InternalService
            
            class PublicApi {
                private val service = InternalService()
                fun execute(): String = service.doWork()
            }
        """.trimIndent())
        
        // StarImportExample - uses star import
        File(otherPkg, "StarImportExample.kt").writeText("""
            package com.acme.other
            
            import com.acme.api.*
            
            class StarImportExample {
                private val api = PublicApi()
                fun run(): String = api.execute()
            }
        """.trimIndent())
        
        // QualifiedRefExample - uses qualified references without import
        File(otherPkg, "QualifiedRefExample.kt").writeText("""
            package com.acme.other
            
            class QualifiedRefExample {
                private val api = com.acme.api.PublicApi()
            }
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder(includePublic = true, includeInternal = true)
            val candidates = finder.findCandidates(result)
            
            val candidateNames = candidates.map { it.declaration.name }.toSet()
            
            // These should be candidates (only used within com.acme.internal)
            assertTrue("InternalHelper" in candidateNames, "InternalHelper should be a candidate")
            assertTrue("utilityFunction" in candidateNames, "utilityFunction should be a candidate")
            assertTrue("InternalObject" in candidateNames, "InternalObject should be a candidate")
            assertTrue("TypeUsageExample" in candidateNames, "TypeUsageExample should be a candidate")
            
            // InternalService is used from com.acme.api - NOT a candidate
            assertTrue("InternalService" !in candidateNames, "InternalService should NOT be a candidate (used cross-package)")
            
            // PublicApi is used from com.acme.other via star import and qualified ref - NOT a candidate
            assertTrue("PublicApi" !in candidateNames, "PublicApi should NOT be a candidate (used cross-package)")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `finds nested class as candidate`() {
        val pkg = File(tempDir, "com/example").apply { mkdirs() }
        
        File(pkg, "Outer.kt").writeText("""
            package com.example
            
            class Outer {
                class Inner {
                    fun doWork(): Int = 42
                }
            }
        """.trimIndent())
        
        File(pkg, "Usage.kt").writeText("""
            package com.example
            
            fun useInner() = Outer.Inner().doWork()
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            // Both Outer and Inner should be candidates (only used within package)
            val candidateNames = candidates.map { it.declaration.name }.toSet()
            assertTrue("Outer" in candidateNames, "Outer should be a candidate")
            assertTrue("Inner" in candidateNames, "Inner should be a candidate")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `excludes nested class used cross-package`() {
        val internalPkg = File(tempDir, "com/example/internal").apply { mkdirs() }
        val apiPkg = File(tempDir, "com/example/api").apply { mkdirs() }
        
        File(internalPkg, "Container.kt").writeText("""
            package com.example.internal
            
            class Container {
                class Nested {
                    fun compute(): Int = 42
                }
            }
        """.trimIndent())
        
        File(apiPkg, "Api.kt").writeText("""
            package com.example.api
            
            import com.example.internal.Container
            
            fun useNested() = Container.Nested().compute()
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            val candidateNames = candidates.map { it.declaration.name }.toSet()
            assertTrue("Container" !in candidateNames, "Container should NOT be a candidate (used cross-package)")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `finds enum class as candidate`() {
        val pkg = File(tempDir, "com/example").apply { mkdirs() }
        
        File(pkg, "Status.kt").writeText("""
            package com.example
            
            enum class Status {
                ACTIVE, INACTIVE, PENDING
            }
        """.trimIndent())
        
        File(pkg, "Service.kt").writeText("""
            package com.example
            
            class Service {
                fun getStatus(): Status = Status.ACTIVE
            }
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            val statusCandidate = candidates.find { it.declaration.name == "Status" }
            assertNotNull(statusCandidate, "Status should be a candidate")
            assertEquals(DeclarationKind.ENUM_CLASS, statusCandidate?.declaration?.kind)
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `finds sealed class as candidate`() {
        val pkg = File(tempDir, "com/example").apply { mkdirs() }
        
        File(pkg, "Result.kt").writeText("""
            package com.example
            
            sealed class Result {
                data class Success(val value: Int) : Result()
                data class Error(val msg: String) : Result()
            }
        """.trimIndent())
        
        File(pkg, "Service.kt").writeText("""
            package com.example
            
            fun process(): Result = Result.Success(42)
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            val resultCandidate = candidates.find { it.declaration.name == "Result" }
            assertNotNull(resultCandidate, "Result should be a candidate")
            assertEquals(DeclarationKind.SEALED_CLASS, resultCandidate?.declaration?.kind)
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `finds typealias as candidate`() {
        val pkg = File(tempDir, "com/example").apply { mkdirs() }
        
        File(pkg, "Types.kt").writeText("""
            package com.example
            
            typealias StringList = List<String>
            typealias Handler = (Int) -> String
        """.trimIndent())
        
        File(pkg, "Service.kt").writeText("""
            package com.example
            
            fun getList(): StringList = listOf("a", "b")
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            val stringListCandidate = candidates.find { it.declaration.name == "StringList" }
            assertNotNull(stringListCandidate, "StringList should be a candidate")
            assertEquals(DeclarationKind.TYPEALIAS, stringListCandidate?.declaration?.kind)
            
            // Handler is not used, so still a candidate
            val handlerCandidate = candidates.find { it.declaration.name == "Handler" }
            assertNotNull(handlerCandidate, "Handler should be a candidate")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `excludes class used in inheritance cross-package`() {
        val basePkg = File(tempDir, "com/example/base").apply { mkdirs() }
        val implPkg = File(tempDir, "com/example/impl").apply { mkdirs() }
        
        File(basePkg, "BaseClass.kt").writeText("""
            package com.example.base
            
            open class BaseClass {
                open fun doWork(): Int = 0
            }
        """.trimIndent())
        
        File(implPkg, "Impl.kt").writeText("""
            package com.example.impl
            
            import com.example.base.BaseClass
            
            class Impl : BaseClass() {
                override fun doWork(): Int = 42
            }
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            val baseCandidate = candidates.find { it.declaration.name == "BaseClass" }
            assertTrue(baseCandidate == null, "BaseClass should NOT be a candidate (used in inheritance cross-package)")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `excludes interface used in inheritance cross-package`() {
        val basePkg = File(tempDir, "com/example/base").apply { mkdirs() }
        val implPkg = File(tempDir, "com/example/impl").apply { mkdirs() }
        
        File(basePkg, "Service.kt").writeText("""
            package com.example.base
            
            interface Service {
                fun execute(): String
            }
        """.trimIndent())
        
        File(implPkg, "ServiceImpl.kt").writeText("""
            package com.example.impl
            
            import com.example.base.Service
            
            class ServiceImpl : Service {
                override fun execute(): String = "done"
            }
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            val serviceCandidate = candidates.find { it.declaration.name == "Service" }
            assertTrue(serviceCandidate == null, "Service interface should NOT be a candidate (implemented cross-package)")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `excludes class used in generic type parameter cross-package`() {
        val modelPkg = File(tempDir, "com/example/model").apply { mkdirs() }
        val servicePkg = File(tempDir, "com/example/service").apply { mkdirs() }
        
        File(modelPkg, "Entity.kt").writeText("""
            package com.example.model
            
            class Entity(val id: Int)
        """.trimIndent())
        
        File(servicePkg, "Repository.kt").writeText("""
            package com.example.service
            
            import com.example.model.Entity
            
            class Repository {
                private val items: List<Entity> = emptyList()
                
                fun getAll(): List<Entity> = items
            }
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            val entityCandidate = candidates.find { it.declaration.name == "Entity" }
            assertTrue(entityCandidate == null, "Entity should NOT be a candidate (used in generic cross-package)")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `excludes annotation used cross-package`() {
        val annotationPkg = File(tempDir, "com/example/annotation").apply { mkdirs() }
        val usagePkg = File(tempDir, "com/example/usage").apply { mkdirs() }
        
        File(annotationPkg, "MyAnnotation.kt").writeText("""
            package com.example.annotation
            
            annotation class MyAnnotation
        """.trimIndent())
        
        File(usagePkg, "Service.kt").writeText("""
            package com.example.usage
            
            import com.example.annotation.MyAnnotation
            
            @MyAnnotation
            class Service {
                fun doWork(): Int = 42
            }
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            val annotationCandidate = candidates.find { it.declaration.name == "MyAnnotation" }
            assertTrue(annotationCandidate == null, "MyAnnotation should NOT be a candidate (used cross-package)")
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `finds extension function as candidate`() {
        val pkg = File(tempDir, "com/example").apply { mkdirs() }
        
        File(pkg, "Extensions.kt").writeText("""
            package com.example
            
            fun String.toTitleCase(): String = this.replaceFirstChar { it.uppercase() }
        """.trimIndent())
        
        File(pkg, "Usage.kt").writeText("""
            package com.example
            
            fun formatName(name: String) = name.toTitleCase()
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            // Extension function should be tracked
            val extensionDecl = result.declarations.find { it.name == "toTitleCase" }
            assertNotNull(extensionDecl, "Extension function should be tracked")
            assertEquals(DeclarationKind.FUNCTION, extensionDecl?.kind)
        } finally {
            analyzer.dispose()
        }
    }

    @Test
    fun `handles complex generic types`() {
        val modelPkg = File(tempDir, "com/example/model").apply { mkdirs() }
        val servicePkg = File(tempDir, "com/example/service").apply { mkdirs() }
        
        File(modelPkg, "Models.kt").writeText("""
            package com.example.model
            
            class Key(val id: String)
            class Value(val data: Int)
        """.trimIndent())
        
        File(servicePkg, "Cache.kt").writeText("""
            package com.example.service
            
            import com.example.model.Key
            import com.example.model.Value
            
            class Cache {
                private val data: Map<Key, List<Value>> = emptyMap()
            }
        """.trimIndent())
        
        val analyzer = SourceAnalyzer()
        try {
            val result = analyzer.analyze(tempDir.walkTopDown().filter { it.extension == "kt" }.toList())
            
            val finder = CandidateFinder()
            val candidates = finder.findCandidates(result)
            
            val keyCandidate = candidates.find { it.declaration.name == "Key" }
            val valueCandidate = candidates.find { it.declaration.name == "Value" }
            
            assertTrue(keyCandidate == null, "Key should NOT be a candidate (used in generic cross-package)")
            assertTrue(valueCandidate == null, "Value should NOT be a candidate (used in generic cross-package)")
        } finally {
            analyzer.dispose()
        }
    }
}
