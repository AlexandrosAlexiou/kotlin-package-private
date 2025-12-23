package com.example.api

import com.example.internal.NativeInternal  // Should fail - cross-package access
import com.example.internal.internalHelper  // Should fail - cross-package access

fun main() {
    val internal = NativeInternal()
    println(internal.publicMethod())
    println(internalHelper())
}
