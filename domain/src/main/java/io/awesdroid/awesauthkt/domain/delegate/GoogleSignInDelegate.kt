package io.awesdroid.awesauthkt.domain.delegate

/**
 * @author Awesdroid
 */
interface GoogleSignInDelegate<in SignInParams, in AuthResponse, out SignInResult, out Token> :
    AuthDelegate<SignInParams, AuthResponse, SignInResult, Token> {
}