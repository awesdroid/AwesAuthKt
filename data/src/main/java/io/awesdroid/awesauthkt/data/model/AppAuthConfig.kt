package io.awesdroid.awesauthkt.data.model

import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import io.awesdroid.libkt.android.gson.UriAdapter

/**
 * @author Awesdroid
 */
data class AppAuthConfig(
    @SerializedName("client_id") var clientId: String?,
    @SerializedName("authorization_scope") var authorizationScope: String?,
    @SerializedName("redirect_uri") var redirectUri: Uri?,
    @SerializedName("discovery_uri") var discoveryUri: Uri?,
    @SerializedName("authorization_endpoint_uri") var authorizationEndpointUri: Uri?,
    @SerializedName("token_endpoint_uri") var tokenEndpointUri: Uri?,
    @SerializedName("registration_endpoint_uri") var registrationEndpointUri: Uri?,
    @SerializedName("user_info_endpoint_uri") var userInfoEndpointUri: Uri?,
    @SerializedName("https_required") val httpsRequired: Boolean
) {

    override fun toString(): String {
        return GsonBuilder()
            .registerTypeAdapter(Uri::class.java, UriAdapter())
            .create()
            .toJson(this)
    }
    companion object {
        fun create(str: String): AppAuthConfig {
            return GsonBuilder()
                .registerTypeAdapter(Uri::class.java, UriAdapter())
                .create()
                .fromJson(str, AppAuthConfig::class.java)
        }
    }
}