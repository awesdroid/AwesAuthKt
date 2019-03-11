package io.awesdroid.awesauthkt.data.exception

/**
 * @author Awesdroid
 */
abstract class AbstractException: Throwable() {
    var reason: Throwable? = null
}