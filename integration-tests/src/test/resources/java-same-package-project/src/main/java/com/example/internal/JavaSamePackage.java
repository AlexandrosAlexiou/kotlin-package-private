package com.example.internal;

// Same package as KotlinInternal - should be allowed to access
public class JavaSamePackage {
    public String access() {
        KotlinInternal internal = new KotlinInternal();
        return internal.publicMethod();
    }
}
