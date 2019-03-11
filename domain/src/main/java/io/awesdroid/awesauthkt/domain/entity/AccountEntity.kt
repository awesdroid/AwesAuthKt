package io.awesdroid.awesauthkt.domain.entity

/**
 * @author Awesdroid
 */
data class AccountEntity(val name: String,
                         val photoUrl: String,
                         val tokenEntity: TokenEntity,
                         val userInfo: String) {
}
