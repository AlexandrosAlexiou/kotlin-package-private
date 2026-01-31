package com.acme.internal

import dev.packageprivate.PackagePrivate

/**
 * Annotation candidate - only used within this package.
 */
@PackagePrivate
annotation class InternalMarker

/**
 * Class using the annotation.
 */
@PackagePrivate
@InternalMarker
class MarkedService {
    @InternalMarker
    fun markedMethod(): Int = 42
}
