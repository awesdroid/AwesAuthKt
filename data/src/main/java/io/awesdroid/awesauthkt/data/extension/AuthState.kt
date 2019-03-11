package io.awesdroid.awesauthkt.data.extension

import io.awesdroid.awesauthkt.domain.entity.AppAuthState
import net.openid.appauth.AuthState

/**
 * @author Awesdroid
 */
fun AuthState.toAppAuthState() = AppAuthState(
    this.lastAuthorizationResponse?.authorizationCode,
    this.refreshToken,
    this.idToken,
    this.accessToken,
    this.accessTokenExpirationTime,
    this.authorizationException,
    this.lastAuthorizationResponse != null
)