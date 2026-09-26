package org.mobilenativefoundation.trails.foundation.logging

// TODO
class Logger {
    fun info(tag: String, message: String) {
        println("$tag: $message")
    }

    fun error(tag: String, message: String, cause: Throwable) {
        println("$tag: $message \n\n Cause: $cause")
    }
}