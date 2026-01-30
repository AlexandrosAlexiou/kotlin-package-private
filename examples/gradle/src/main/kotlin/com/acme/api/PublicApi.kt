package com.acme.api

import com.acme.internal.InternalService

/**
 * This is the public API that uses InternalService.
 * 
 * Note: InternalService is used from a different package (com.acme.api),
 * so it will NOT be suggested as a @PackagePrivate candidate.
 * 
 * However, InternalHelper and utilityFunction (used only within com.acme.internal)
 * WILL be suggested as candidates.
 */
class PublicApi {
    private val service = InternalService()
    
    fun execute(): String = service.doWork()
}
