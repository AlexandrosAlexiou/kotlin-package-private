package com.acme.internal

import dev.packageprivate.PackagePrivate

/**
 * Property candidate - only used within this package.
 */
@PackagePrivate
val internalConfig: String = "config-value"

/**
 * Extension function candidate - only used within this package.
 */
@PackagePrivate
fun String.toInternalFormat(): String = "[$this]"

/**
 * Extension property candidate - only used within this package.
 */
@PackagePrivate
val String.internalLength: Int
    get() = this.length * 2
