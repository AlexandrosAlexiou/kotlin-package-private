package com.example

import com.example.api.PublicApi

fun main() {
  val api = PublicApi()
  val result = api.calculate(5, 3)
  
  console.log("Result: $result")
  console.log("Package-private annotation enforces compile-time access control!")
}
