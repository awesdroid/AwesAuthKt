package io.awesdroid.awesauthkt.repository

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.GsonBuilder
import io.awesdroid.awesauthkt.R
import io.awesdroid.awesauthkt.db.AuthDatabase
import io.awesdroid.awesauthkt.db.entity.ConfigEntity
import io.awesdroid.awesauthkt.db.entity.StateEntity
import io.awesdroid.awesauthkt.model.AppAuthConfig
import io.awesdroid.awesauthkt.model.AppAuthState
import io.awesdroid.awesauthkt.service.AppAuthService
import io.awesdroid.libkt.android.gson.UriAdapter
import io.awesdroid.libkt.common.utils.TAG
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import net.openid.appauth.AuthState
import org.json.JSONObject
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

/**
 * @author Awesdroid
 */
class AppAuthRepository(private var context: Context?) {
    private var appAuthService: AppAuthService? = null

    // Live Data
    private val appAuthState: MutableLiveData<AppAuthState> = MutableLiveData<AppAuthState>(null)
    private val userInfo: MutableLiveData<JSONObject> = MutableLiveData<JSONObject>(null)
    private val error: MutableLiveData<Exception>? = null

    private val appAuthConfig: Single<AppAuthConfig>
        get() = Single.defer<AppAuthConfig> {
            with(AuthDatabase.instance!!.configDao().loadConfig(1)) {
                when(this) {
                    null -> {
                        Log.d(TAG, "getAppAuthConfig(): read raw config")
                        readConfig().map {
                            AuthDatabase.instance!!.configDao().insertConfig(ConfigEntity(1, it))
                        it
                        }
                    }
                    else -> Single.just(this.appAuthConfig)
                }
            }
        }.subscribeOn(Schedulers.io())

    @SuppressLint("CheckResult")
    fun init(completeActivity: Activity, cancelActivity: Activity) {
        appAuthService = AppAuthService()
        appAuthConfig.subscribe { config ->
            var authState: AuthState? = AuthDatabase.instance!!.stateDao().loadAppAuthState(1)?.let { saved ->
                Log.d(TAG, "init(): load saved state = ${saved.appAuthState}")
                appAuthState.postValue(saved.appAuthState)
                saved.appAuthState!!.authState
            }
            Log.d(TAG, "init(): authState = $authState")
            appAuthService!!.init(context!!, completeActivity, cancelActivity, config, authState)
        }
    }

    fun getAppAuthState(): LiveData<AppAuthState> {
        return appAuthState
    }

    fun getUserInfo(): LiveData<JSONObject> {
        return userInfo
    }

    fun getError(): LiveData<Exception>? {
        return error
    }

    @SuppressLint("CheckResult")
    fun handleAuthResponse(intent: Intent) {
        appAuthService!!.handleAuthResponse(intent)
            .observeOn(Schedulers.io())
            .subscribe ( { this.updateState(it) }, { e -> this.handleError(e) })
    }

    fun signIn(usePendingIntent: Boolean, requestCode: Int) {
        CompletableFuture.runAsync { appAuthService!!.doAuth(usePendingIntent, requestCode) }
    }

    @SuppressLint("CheckResult")
    fun fetchUserInfo() {
        appAuthService!!.fetchUserInfo()
            .observeOn(Schedulers.trampoline())
            .subscribe(
                { info -> userInfo.postValue(info) },
                { this.handleError(it) },
                { userInfo.postValue(null) })
    }

    @SuppressLint("CheckResult")
    fun refreshToken() {
        appAuthService!!.refreshAccessToken()
            .observeOn(Schedulers.io())
            .subscribe({ this.updateState(it) }, { this.handleError(it) } )
    }

    @SuppressLint("CheckResult")
    fun signOut() {
        appAuthService!!.signOut()
            .observeOn(Schedulers.single())
            .subscribe { it -> this.updateState(it) }
    }

    fun destroy() {
        Log.d(TAG, "destroy()")
        AuthDatabase.instance?.destroy()
        appAuthService?.destroy()
        appAuthService = null
        context = null
    }

    private fun readConfig(): Single<AppAuthConfig> {
        return Single.just(R.raw.google_config)
            .map { context!!.resources.openRawResource(it) }
            .map { InputStreamReader(it, StandardCharsets.UTF_8) }
            .map { GsonBuilder().registerTypeAdapter(Uri::class.java, UriAdapter()).create()
                .fromJson(it, AppAuthConfig::class.java) }
            .subscribeOn(Schedulers.io())
    }

    private fun updateState(authState: AuthState) {
        with(appAuthState.value) {
            this?.apply { this.authState = authState  }?:AppAuthState(authState)
        }.let {
            updateStateDb(it)
            appAuthState.postValue(it)
            Log.d(TAG, "updateState(): authState = $authState")
        }

    }

    private fun updateStateDb(appAuthState: AppAuthState) {
        CompletableFuture.runAsync {
            AuthDatabase.instance!!
                .stateDao()
                .insertAppAuthState(StateEntity(1, appAuthState))
        }
    }

    private fun handleError(e: Throwable) {
        // TODO
        e.printStackTrace()
    }
}
