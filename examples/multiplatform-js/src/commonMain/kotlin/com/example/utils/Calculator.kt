package com.example.utils

class Calculator {
  fun add(a: Int, b: Int): Int {
    // ✅ Same package - allowed to use @PackagePrivate members
    val helper = InternalHelper()
    return helper.compute(a, b)
  }
  
  fun getMessage(): String {
    // ✅ Can use package-private function within same package
    return utilityFunction()
  }
}
