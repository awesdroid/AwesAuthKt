package io.awesdroid.awesauthkt.net

import android.util.Log
import io.awesdroid.libkt.common.utils.TAG
import io.reactivex.Maybe
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.regex.Pattern

/**
 * @auther Awesdroid
 */
object Http {
    fun getUserInfo(url: String, token: String): Maybe<String> {
        Log.d(TAG, "getUserInfo: token = $token\nurl=$url")
        val baseUrl = getBaseUrl(url)
        val client = OkHttpClient.Builder().build()
        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(baseUrl)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()

        val api = retrofit.create(Api::class.java)
        return Maybe.create { emitter ->
            api.getUerInfo(url, "Bearer $token")
                .subscribe(
                    { emitter.onSuccess(it) },
                    { e -> emitter.onError(handleError(e)) },
                    { emitter.onComplete() })
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun getBaseUrl(url: String): String {
        val pattern = Pattern
            .compile("(http://|ftp://|https://|www)?[^\u4e00-\u9fa5\\s]*?/")
        val matcher = pattern.matcher(url)
        if (matcher.find()) {
            Log.d(TAG, "getBaseUrl(): " + matcher.group(0))
            return matcher.group(0)
        }
        throw IllegalArgumentException("Can't get baseURL from: $url")
    }

    private fun handleError(e: Throwable): Throwable {
        // TODO
        return e
    }
}