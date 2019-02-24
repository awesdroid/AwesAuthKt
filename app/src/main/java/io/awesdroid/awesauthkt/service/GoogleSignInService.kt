package io.awesdroid.awesauthkt.service

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.gson.GsonBuilder
import io.awesdroid.awesauthkt.R
import io.awesdroid.awesauthkt.model.GoogleSignInConfig
import io.awesdroid.libkt.android.exceptions.LiveException
import io.awesdroid.libkt.android.exceptions.LiveExceptionHandler
import io.awesdroid.libkt.common.executors.Dispatchers.CACHED
import io.awesdroid.libkt.common.utils.TAG
import io.reactivex.Completable
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.rxCompletable
import kotlinx.coroutines.rx2.rxSingle
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * @author Awesdroid
 */
class GoogleSignInService: CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = CACHED

    private val errorHandler = LiveExceptionHandler()

    private var activity: Activity? = null
    private var googleSignInClient: GoogleSignInClient? = null
    private var googleSinInConfig: GoogleSignInConfig? = null

    fun init(activity: Activity) {
        this.activity = activity
        initConfig()
    }

    @SuppressLint("CheckResult")
    private fun initConfig() {
        Log.d(TAG, "initConfig(): googleSinInConfig = $googleSinInConfig")
        launch(errorHandler.handler) {
            googleSinInConfig?:run {
                InputStreamReader(activity!!.resources.openRawResource(R.raw.google_signin), StandardCharsets.UTF_8)
                    .let { GsonBuilder().create().fromJson(it, GoogleSignInConfig::class.java) }
                    .run {
                        Log.d(TAG, "initConfig: this = $this")
                        googleSinInConfig = this }
            }
        }
    }

    fun doAuth(isGoogleSignInUseIdToken: Boolean, responseCode: Int) {
        googleSignInClient = if (isGoogleSignInUseIdToken) {
            Log.d(TAG, "doAuth(): googleSinInConfig!!.client_id = ${googleSinInConfig!!.client_id}")
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(googleSinInConfig!!.client_id)
                .requestEmail()
                .build()
            GoogleSignIn.getClient(activity!!, gso)
        } else {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
            GoogleSignIn.getClient(requireNotNull(activity), gso)
        }
        val signInIntent = googleSignInClient!!.signInIntent
        activity!!.startActivityForResult(signInIntent, responseCode)
    }

    fun signOut(): Completable {
        return rxCompletable {
            suspendCoroutine { continuation ->
                googleSignInClient!!.signOut()
                    .addOnCompleteListener { continuation.resume(Unit) }
            }
        }
    }

    fun refreshToken(): Single<GoogleSignInAccount> {
        return rxSingle {
            suspendCoroutine<GoogleSignInAccount> { continuation ->
                googleSignInClient!!.silentSignIn()
                    .addOnCompleteListener { task ->
                        task.result?.run {
                            Log.d(TAG, "refreshToken: this = $this")
                            continuation.resume(this)
                        }?:run {
                            (errorHandler.getError() as MutableLiveData<LiveException>)
                                .postValue(LiveException(LiveException.Type.ERROR_GENERAL, Throwable("Empty token")))
                        }
                    }
            }
        }

    }

    fun destroy() {
        activity = null
        googleSignInClient = null
    }

    fun getError(): LiveData<LiveException> {
        return errorHandler.getError()
    }

}
