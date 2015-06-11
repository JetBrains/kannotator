package org.jetbrains.kannotator

interface ErrorHandler {
    fun error(message: String)
    fun warning(message: String)
}

val PRINT_TO_CONSOLE = simpleErrorHandler {
    kind, message -> System.err.println("$kind: $message")
}

val NO_ERROR_HANDLING = simpleErrorHandler {
    kind, message ->
}

enum class ErrorKind {
    ERROR,
    WARNING
}

fun simpleErrorHandler(handler: (kind: ErrorKind, message: String) -> Unit): ErrorHandler {
    return object : ErrorHandler {
        override fun error(message: String) {
            handler(ErrorKind.ERROR, message)
        }

        override fun warning(message: String) {
            handler(ErrorKind.WARNING, message)
        }
    }
}