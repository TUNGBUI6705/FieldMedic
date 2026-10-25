package com.example.fieldmedic.domain.model

sealed class AppError : Throwable() {
    class DatabaseLocked : AppError()
    data class NetworkError(override val message: String) : AppError()
    data class Unauthorized(override val message: String) : AppError()
    data class ConflictDetected(val conflictId: String) : AppError()
    data class ValidationFailed(val reason: String) : AppError()
    data class Unknown(override val cause: Throwable) : AppError()
}
