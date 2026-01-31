package com.acme.internal

import dev.packageprivate.PackagePrivate

/** Enum class candidate - only used within this package. */
enum class InternalStatus {
    ACTIVE,
    INACTIVE,
    PENDING,
}

/** Sealed class candidate - only used within this package. */
sealed class InternalResult {
    data class Success(val value: Int) : InternalResult()

    data class Error(val message: String) : InternalResult()
}

/** Typealias candidate - only used within this package. */
@PackagePrivate
typealias InternalCallback = (Int) -> String
