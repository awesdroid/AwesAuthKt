package io.awesdroid.awesauthkt.model

import io.awesdroid.awesauthkt.utils.Utils
import net.openid.appauth.AuthState

/**
 * @auther Awesdroid
 */
data class AppAuthState(var authState: AuthState?) {
    val authorizationCode: String?
        get() = authState?.lastAuthorizationResponse?.authorizationCode

    val refreshToken: String?
        get() = authState?.refreshToken

    val idToken: String?
        get() = authState?.idToken

    val accessToken: String?
        get() = authState?.accessToken

    val accessTokenExpirationTime: Long?
        get() = authState?.accessTokenExpirationTime

    val authorizationException: Exception?
        get() = authState?.authorizationException

    val hasLastTokenResponse: Boolean
        get() = authState?.lastTokenResponse != null

    override fun toString(): String {
        return Utils.prettyJson(authState!!.jsonSerializeString())
    }
}