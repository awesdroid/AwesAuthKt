package io.awesdroid.awesauthkt.domain.entity

/**
 * @author Awesdroid
 */
data class AppAuthState(val authorizationCode: String?,
                        val refreshToken: String?,
                        val idToken: String?,
                        val accessToken: String?,
                        val accessTokenExpirationTime: Long?,
                        val authorizationException: Exception?,
                        val hasLastTokenResponse: Boolean) {
    companion object {
        fun empty() = AppAuthState(null,
            null,
            null,
            null,
            null,
            null,
            false)
    }

}