package io.awesdroid.awesauthkt.data.exception

/**
 * @author Awesdroid
 */
sealed class RecoverableException(): AbstractException() {
    constructor(e: Throwable): this() { reason = e }

    data class CommonError(val e: Throwable): RecoverableException(e)
    data class NetworkError(val e: Throwable): RecoverableException(e)
    data class DatabaseError(val e: Throwable): RecoverableException(e)

    object EmptyError: RecoverableException()
}