package com.acme.other

import dev.packageprivate.PackagePrivate

// Star import - the analyzer tracks this correctly
import com.acme.api.*

/**
 * This file uses star imports.
 * The analyzer correctly detects PublicApi usage through the star import.
 */
@PackagePrivate
class StarImportExample {
    private val api = PublicApi()
    
    fun run(): String = api.execute()
}
