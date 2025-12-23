package com.example.internal

// Same package - should be allowed to access @PackagePrivate declarations
fun main() {
    val internal = NativeInternal()
    println(internal.secret())      // Same package - allowed
    println(internal.publicMethod())
    println(internalHelper())       // Same package - allowed
}
