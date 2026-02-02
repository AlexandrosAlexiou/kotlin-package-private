package com.acme.foo

import dev.packageprivate.PackagePrivate

@PackagePrivate
class HiddenKotlinClass {
  @PackagePrivate  // Redundant - class is already @PackagePrivate
  fun helper(): String = "package-private from Kotlin"
}
