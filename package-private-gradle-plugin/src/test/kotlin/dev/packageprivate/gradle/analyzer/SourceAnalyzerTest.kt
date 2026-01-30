package dev.packageprivate.gradle.analyzer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
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
}
