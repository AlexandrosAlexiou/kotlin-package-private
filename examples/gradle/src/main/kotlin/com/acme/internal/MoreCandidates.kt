package com.acme.internal

import dev.packageprivate.PackagePrivate

/** Enum class candidate - only used within this package. */
@PackagePrivate
enum class InternalStatus {
    ACTIVE,
    INACTIVE,
    PENDING,
}

/** Sealed class candidate - only used within this package. */
@PackagePrivate
sealed class InternalResult {
    data class Success(val value: Int) : InternalResult()

    data class Error(val message: String) : InternalResult()
}

/** Typealias with @PackagePrivate - demonstrates annotation on typealias. */
@PackagePrivate
typealias InternalCallback = (Int) -> String
