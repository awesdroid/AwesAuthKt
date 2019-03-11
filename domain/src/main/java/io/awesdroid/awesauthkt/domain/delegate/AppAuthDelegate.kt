package io.awesdroid.awesauthkt.domain.delegate

/**
 * @author Awesdroid
 */
interface AppAuthDelegate<in InitParams, in SignInParams, in AuthResponse, out SignInResult, out Token, out UserInfo> :
    AuthDelegate<SignInParams, AuthResponse, SignInResult, Token> {
    fun initState(params: InitParams): Token
    fun getUserInfo(): UserInfo
    fun clear()
}