package io.awesdroid.awesauthkt.model

import com.google.gson.GsonBuilder

/**
 * @author Awesdroid
 */
class GoogleSignInConfig {
    val client_id: String? = null

    override fun toString(): String {
        return GsonBuilder()
            .create()
            .toJson(this)
    }
}
