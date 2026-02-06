package com.example.api

import com.example.internal.Consumer

// Uses PublicApi from same package and Consumer from different package
class Service {
    fun run() {
        val api = PublicApi()
        val consumer = Consumer()
    }
}
