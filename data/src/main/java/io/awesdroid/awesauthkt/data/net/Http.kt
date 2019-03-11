package io.awesdroid.awesauthkt.data.net

import android.util.Log
import io.awesdroid.awesauthkt.data.di.DI
import io.awesdroid.awesauthkt.data.exception.RecoverableException
import io.awesdroid.libkt.common.utils.TAG
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.rx2.await
import org.kodein.di.generic.instance
import retrofit2.Retrofit

/**
 * @author Awesdroid
 */
object Http {
    suspend fun getUserInfo(url: String, token: String): String? = coroutineScope {
        try {
            Log.d(TAG, "getUserInfo: token = $token\nurl=$url")
            val retrofit: Retrofit by DI.kodein!!.instance(null, url)
            retrofit.create(Api::class.java)
                .getUerInfo(url, "Bearer $token")
                .await()
        } catch (e: Throwable) {
            throw RecoverableException.NetworkError(e)
        }
    }
}