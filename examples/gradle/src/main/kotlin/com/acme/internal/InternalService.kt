package com.acme.internal

/**
 * This service uses InternalHelper and utilityFunction.
 * Since all usages are within the same package, the analyzer
 * will suggest adding @PackagePrivate to InternalHelper and utilityFunction.
 */
class InternalService {
    private val helper = InternalHelper()
    
    fun doWork(): String {
        val result = helper.compute()
        val util = utilityFunction()
        return "Result: $result, Util: $util"
    }
}
