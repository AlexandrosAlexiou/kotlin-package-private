package com.acme.internal

import dev.packageprivate.PackagePrivate

/**
 * This object is a good candidate for @PackagePrivate!
 * It's only used within this package.
 */
@PackagePrivate
object InternalObject {
    fun getInstance(): String = "singleton"
}
