package com.example.internal

// Uses InternalHelper, InternalService, utilityFunction - all in same package
class Consumer {
    fun execute() {
        val helper = InternalHelper()
        val service = InternalService()
        val result = utilityFunction()
    }
}
