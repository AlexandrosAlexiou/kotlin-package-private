package com.acme.internal

import dev.packageprivate.PackagePrivate

/**
 * Base class for inheritance example - only extended within this package.
 */
@PackagePrivate
open class InternalBase {
    open fun doWork(): Int = 0
}

/**
 * Interface for inheritance example - only implemented within this package.
 */
@PackagePrivate
interface InternalContract {
    fun execute(): String
}

// Implementation within same package
@PackagePrivate
class InternalImpl : InternalBase(), InternalContract {
    override fun doWork(): Int = 42
    override fun execute(): String = "done"
}
