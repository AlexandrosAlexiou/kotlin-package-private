package com.example.internal

import com.acme.packageprivate.PackagePrivate

// This class has both package-private and public members
class MixedVisibility {
    @PackagePrivate
    fun packagePrivateMethod(): String = "secret"
    
    fun publicMethod(): String = "public"
    
    @PackagePrivate
    val packagePrivateProperty: String = "secret property"
    
    val publicProperty: String = "public property"
}

// This class is fully public (no @PackagePrivate)
class FullyPublic {
    fun accessibleMethod(): String = "accessible"
    
    val accessibleProperty: String = "accessible property"
}
