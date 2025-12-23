package com.example.internal

import com.acme.packageprivate.PackagePrivate

@PackagePrivate
class KotlinInternal {
    @PackagePrivate
    fun secret(): String = "secret"
    
    fun publicMethod(): String = "public"
}
