package io.awesdroid.awesauthkt.model

import android.net.Uri
import com.google.gson.GsonBuilder

/**
 * @auther Awesdroid
 */
class AppAuthConfig internal constructor() {

    val client_id: String? = null
    val authorization_scope: String? = null
    val redirect_uri: Uri? = null
    val discovery_uri: Uri? = null
    val authorization_endpoint_uri: Uri? = null
    val token_endpoint_uri: Uri? = null
    val registration_endpoint_uri: Uri? = null
    val user_info_endpoint_uri: Uri? = null
    val https_required: Boolean = false

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