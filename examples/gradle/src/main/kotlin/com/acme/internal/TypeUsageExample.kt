package com.acme.internal

import dev.packageprivate.PackagePrivate

/**
 * This file demonstrates type reference detection.
 * The analyzer detects InternalHelper usage in type positions.
 */
@PackagePrivate
class TypeUsageExample {
    // Type reference: InternalHelper used as a property type
    private val helper: InternalHelper = InternalHelper()
    
    // Type reference: InternalHelper used as parameter type
    fun process(h: InternalHelper): Int = h.compute()
    
    // Type reference: InternalHelper used as return type
    fun createHelper(): InternalHelper = InternalHelper()
    
    // Using InternalObject
    fun getSingleton(): String = InternalObject.getInstance()
}
