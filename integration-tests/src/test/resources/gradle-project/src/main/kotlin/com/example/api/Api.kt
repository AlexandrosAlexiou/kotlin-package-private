package com.example.api

import com.example.internal.Helper
import com.example.internal.InternalHandler

// This violates package-private - should cause compilation error
fun createHelper() = Helper()

// This also violates package-private on typealias
fun useHandler(h: InternalHandler) = h(42)
