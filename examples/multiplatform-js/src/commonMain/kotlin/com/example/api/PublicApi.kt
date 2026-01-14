package com.example.api

// Uncomment to see the compiler plugin fail the build:
// import com.example.utils.InternalHelper
// import com.example.utils.utilityFunction

import com.example.utils.Calculator

class PublicApi {
  fun calculate(x: Int, y: Int): Int {
    // ✅ Calculator is public, so we can use it
    return Calculator().add(x, y)
  }
  
  // ❌ Cannot access InternalHelper - it's package-private in com.example.utils
  // fun broken() = InternalHelper()
  
  // ❌ Cannot access utilityFunction - it's package-private in com.example.utils
  // fun alsobroken() = utilityFunction()
}
