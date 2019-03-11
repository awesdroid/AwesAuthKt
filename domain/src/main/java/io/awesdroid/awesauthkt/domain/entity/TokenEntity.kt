package io.awesdroid.awesauthkt.domain.entity

/**
 * @author Awesdroid
 */
data class TokenEntity(val idToken: String,
                       val isExpired: Boolean,
                       val expiredTime: Long,
                       val refreshToken: String) {
    companion object {
        fun empty() = TokenEntity("", true, 0L, "")
    }
}