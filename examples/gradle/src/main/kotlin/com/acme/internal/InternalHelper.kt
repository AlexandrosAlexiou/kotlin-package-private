package com.acme.internal

import dev.packageprivate.PackagePrivate

/**
 * This class is a good candidate for @PackagePrivate!
 * It's public but only used within this package.
 * 
 * Run: ./gradlew :examples:example-gradle:analyzePackagePrivateCandidates
 * to see this reported as a candidate.
 */
@PackagePrivate
class InternalHelper {
    fun compute(): Int = 42
}

/**
 * This function is also a candidate - internal but only used in this package.
 */
@PackagePrivate
internal fun utilityFunction(): String = "utility"
