package io.awesdroid.awesauthkt.data.model

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

/**
 * @author Awesdroid
 */
data class GoogleSignInConfig(
    @SerializedName("client_id") val clientId: String
) {
    override fun toString(): String {
        return GsonBuilder()
            .create()
            .toJson(this)
    }
}
