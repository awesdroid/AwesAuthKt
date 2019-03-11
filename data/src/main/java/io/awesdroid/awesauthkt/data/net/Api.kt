package io.awesdroid.awesauthkt.data.net

import io.reactivex.Maybe
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

/**
 * @author Awesdroid
 */
interface Api {
    @GET
    fun getUerInfo(@Url url: String, @Header("Authorization") authorization: String): Maybe<String>
}