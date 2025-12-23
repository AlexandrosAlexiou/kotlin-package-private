package com.example.other;

import com.example.internal.KotlinInternal;

// This Java class tries to access @PackagePrivate Kotlin code
// Should fail to compile due to internal visibility
public class JavaAccessor {
    public String tryAccess() {
        KotlinInternal internal = new KotlinInternal();
        return internal.publicMethod();
    }
}
