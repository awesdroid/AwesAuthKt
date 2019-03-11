package io.awesdroid.awesauthkt.presentation.model

/**
 * @author Awesdroid
 */
data class UserAccount(val name: String,
                       val photoUrl: String,
                       val idToken: String,
                       val expiredTime: Long,
                       val userInfo: String) {
    companion object {
        fun empty() = UserAccount("", "", "", 0L, "")
    }
}
