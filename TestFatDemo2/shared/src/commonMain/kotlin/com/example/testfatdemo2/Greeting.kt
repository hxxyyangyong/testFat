package com.example.testfatdemo2

class Greeting {
    fun greeting(): String {
        return "Hello, ${Platform().platform}!"
    }
}