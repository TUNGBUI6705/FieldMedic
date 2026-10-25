package com.example.fieldmedic.domain.model

sealed interface AppResult<out T, out E : AppError> {
    data class Success<out T>(val data: T) : AppResult<T, Nothing>
    data class Error<out E : AppError>(val error: E) : AppResult<Nothing, E>

    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): E? = (this as? Error)?.error

    fun <R> map(transform: (T) -> R): AppResult<R, E> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }
}
