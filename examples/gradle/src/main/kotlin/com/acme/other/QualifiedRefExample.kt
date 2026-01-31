package com.acme.other

import dev.packageprivate.PackagePrivate

/**
 * This file uses fully qualified references (no import).
 * The analyzer correctly detects these usages.
 */
@PackagePrivate
class QualifiedRefExample {
    // Qualified reference without import
    private val api = com.acme.api.PublicApi()

    // Also works for function calls
    fun run(): String = com.acme.api.PublicApi().execute()
}
