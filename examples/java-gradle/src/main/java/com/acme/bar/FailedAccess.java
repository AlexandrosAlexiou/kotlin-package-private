package com.acme.bar;

import com.acme.foo.PublicKotlinClass;
// Uncomment to see the compiler plugin fail the build.
// import com.acme.foo.HiddenKotlinClass;

public class FailedAccess {
  public static void main(String[] args) {
    // This should work - accessing public Kotlin class from another package
    PublicKotlinClass pub = new PublicKotlinClass();
    System.out.println("Cross-package public access: " + pub.publicMethod());

    // Uncomment to see compiler error - Java cannot access @PackagePrivate from different package
    // HiddenKotlinClass hidden = new HiddenKotlinClass();
    // System.out.println("This should fail: " + hidden.helper());
  }
}
