package io.awesdroid.awesauthkt.net

import io.reactivex.Maybe
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

/**
 * @auther Awesdroid
 */
interface Api {
    @GET
    fun getUerInfo(@Url url: String, @Header("Authorization") authorization: String): Maybe<String>
}