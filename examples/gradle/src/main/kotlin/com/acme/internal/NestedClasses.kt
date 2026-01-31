package com.acme.internal

import dev.packageprivate.PackagePrivate

/**
 * Nested class example - Container.Nested is only used within this package.
 */
@PackagePrivate
class Container {
    class Nested {
        fun compute(): Int = 42
    }
    
    // Inner class (has reference to outer)
    inner class Inner {
        fun getOuter(): Container = this@Container
    }
}
