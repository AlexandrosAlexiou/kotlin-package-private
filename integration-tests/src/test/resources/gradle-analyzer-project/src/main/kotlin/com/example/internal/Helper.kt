package com.example.internal

// Should be found as candidate - only used in same package
class InternalHelper {
    fun doWork() = "internal"
}

// Should be found as candidate - only used in same package
internal class InternalService {
    fun process() = "processing"
}

// Should be found as candidate - only used in same package
fun utilityFunction(): String = "utility"
