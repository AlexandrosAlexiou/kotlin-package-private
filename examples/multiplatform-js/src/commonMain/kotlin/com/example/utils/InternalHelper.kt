package com.example.utils

import dev.packageprivate.PackagePrivate

@PackagePrivate
class InternalHelper {
  fun compute(x: Int, y: Int): Int = x + y
  
  val secretValue: String = "This is package-private"
}

@PackagePrivate
fun utilityFunction(): String = "Package-private utility"
