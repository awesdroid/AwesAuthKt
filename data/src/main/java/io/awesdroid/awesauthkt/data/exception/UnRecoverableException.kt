package io.awesdroid.awesauthkt.data.exception

/**
 * @author Awesdroid
 */
sealed class UnRecoverableException(): AbstractException() {
    constructor(e: Throwable): this() { reason = e }

    data class FatalError(val e: Throwable): UnRecoverableException(e)
    data class DatabaseError(val e: Throwable): UnRecoverableException(e)
}