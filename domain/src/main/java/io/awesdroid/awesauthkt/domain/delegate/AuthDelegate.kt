package io.awesdroid.awesauthkt.domain.delegate

/**
 * @author Awesdroid
 */
interface AuthDelegate<in SignInParams, in AuthResponse, out SignInResult, out Token> {
    fun signIn(params: SignInParams)
    fun signOut(): Token
    fun handleAuthResponse(response: AuthResponse): SignInResult
    fun refreshToken(): Token
}