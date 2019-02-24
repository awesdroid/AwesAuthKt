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
import io.awesdroid.libkt.android.exceptions.LiveException
import io.awesdroid.libkt.android.exceptions.LiveExceptionHandler
import io.awesdroid.libkt.android.gson.UriAdapter
import io.awesdroid.libkt.common.executors.Dispatchers.CACHED
import io.awesdroid.libkt.common.utils.TAG
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.openid.appauth.AuthState
import org.json.JSONObject
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * @author Awesdroid
 */
class AppAuthRepository(private var context: Context?): CoroutineScope {
    private var appAuthService: AppAuthService? = null
    override val coroutineContext: CoroutineContext
        get() = CACHED

    // Live Data
    private val appAuthState: MutableLiveData<AppAuthState> = MutableLiveData<AppAuthState>(null)
    private val userInfo: MutableLiveData<JSONObject> = MutableLiveData<JSONObject>(null)
    private val errorHandler = LiveExceptionHandler()

    private var appAuthConfig: AppAuthConfig = AppAuthConfig()

    @SuppressLint("CheckResult")
    fun init(completeActivity: Activity, cancelActivity: Activity) {
        launch(errorHandler.handler) {
            appAuthService = AppAuthService()
            appAuthConfig = suspendCoroutine { continuation ->
                AuthDatabase.instance!!.configDao().loadConfig(1)
                    ?.let { continuation.resume(it.appAuthConfig) }
                    ?:let {
                        readConfig().subscribe ({ config ->
                            AuthDatabase.instance!!.configDao().insertConfig(ConfigEntity(1, config))
                            continuation.resume(config)
                        }, {
                            continuation.resumeWithException(it)
                        })
                    }
            }

            val authState = AuthDatabase.instance!!.stateDao().loadAppAuthState(1)?.let { saved ->
                Log.d(TAG, "init(): load saved state = ${saved.appAuthState}")
                appAuthState.postValue(saved.appAuthState)
                saved.appAuthState!!.authState
            }
            Log.d(TAG, "init(): authState = $authState")
            appAuthService!!.init(context!!, completeActivity, cancelActivity, appAuthConfig, authState)
        }
    }

    fun getAppAuthState(): LiveData<AppAuthState> {
        return appAuthState
    }

    fun getUserInfo(): LiveData<JSONObject> {
        return userInfo
    }

    fun getError(): LiveData<LiveException> {
        return errorHandler.getError()
    }

    @SuppressLint("CheckResult")
    fun handleAuthResponse(intent: Intent) {
        appAuthService!!.handleAuthResponse(intent)
            .observeOn(Schedulers.io())
            .subscribe ( { this.updateState(it) }, { e -> this.handleError(e) })
    }

    fun signIn(usePendingIntent: Boolean, requestCode: Int) {
        launch(errorHandler.handler) {
            appAuthService!!.doAuth(usePendingIntent, requestCode)
        }
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
            .subscribe (
                { updateState(it) },
                { handleError(it) }
            )
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
        launch(errorHandler.handler) {
            AuthDatabase.instance!!.stateDao().insertAppAuthState(StateEntity(1, appAuthState))
        }
    }

    private fun handleError(e: Throwable)  = launch(errorHandler.handler) {
        Log.e(TAG, "AppAuthRepository: handleError(): e = $e")
        throw e
    }
}
