package io.awesdroid.awesauthkt.data.di

import android.content.ContentValues.TAG
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.kodein.di.Kodein
import org.kodein.di.generic.*
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

const val HTTP_MODULE_TAG = "http_module"
const val HTTP_INTERCEPTOR_LOG_TAG = "http_interceptor_log_tag"

const val TIME_OUT_SECONDS = 20

val httpModule = Kodein.Module(HTTP_MODULE_TAG) {

    bind<Retrofit.Builder>() with provider { Retrofit.Builder() }

    bind<OkHttpClient.Builder>() with provider { OkHttpClient.Builder() }

    bind<OkHttpClient>() with singleton {
        instance<OkHttpClient.Builder>()
            .connectTimeout(TIME_OUT_SECONDS.toLong(), TimeUnit.SECONDS)
            .readTimeout(TIME_OUT_SECONDS.toLong(), TimeUnit.SECONDS)
            .addInterceptor(instance(HTTP_INTERCEPTOR_LOG_TAG))
            .build()
    }
    bind<Retrofit>() with factory { url: String ->
        instance<Retrofit.Builder>()
                .baseUrl(getBaseUrl(url))
                .client(instance())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
    }

    bind<Interceptor>(HTTP_INTERCEPTOR_LOG_TAG) with singleton {
        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
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