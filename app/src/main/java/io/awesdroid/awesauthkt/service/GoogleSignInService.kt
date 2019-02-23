package io.awesdroid.awesauthkt.service

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.gson.GsonBuilder
import io.awesdroid.awesauthkt.R
import io.awesdroid.awesauthkt.model.GoogleSignInConfig
import io.awesdroid.libkt.common.utils.TAG
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * @auther Awesdroid
 */
class GoogleSignInService {
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
        googleSinInConfig?:run{
            Single.just(R.raw.google_signin)
                .observeOn(Schedulers.io())
                .map { activity!!.resources.openRawResource(it) }
                .map { InputStreamReader(it, StandardCharsets.UTF_8) }
                .map { GsonBuilder().create().fromJson(it, GoogleSignInConfig::class.java) }
                .subscribe { config -> googleSinInConfig = config }
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
        return Completable.create { v ->
            googleSignInClient!!.signOut()
                .addOnCompleteListener { v.onComplete() }
        }
    }

    fun refreshToken(): Observable<GoogleSignInAccount> {
        return Observable.create { v ->
            googleSignInClient!!.silentSignIn()
                .addOnCompleteListener { task ->
                    v.onNext(task.result!!)
                    v.onComplete()
                }
        }

    }

    fun destroy() {
        activity = null
        googleSignInClient = null
    }

}
