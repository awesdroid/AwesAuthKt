package io.awesdroid.awesauthkt.data.delegate

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.gson.GsonBuilder
import io.awesdroid.awesauthkt.data.R
import io.awesdroid.awesauthkt.data.exception.RecoverableException
import io.awesdroid.awesauthkt.data.model.GoogleSignInConfig
import io.awesdroid.awesauthkt.domain.delegate.GoogleSignInDelegate
import io.awesdroid.awesauthkt.domain.entity.AccountEntity
import io.awesdroid.awesauthkt.domain.entity.TokenEntity
import io.awesdroid.libkt.common.executors.Dispatchers
import io.awesdroid.libkt.common.utils.prettyJsonObject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.rxSingle
import org.json.JSONObject
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * @author Awesdroid
 */
class GoogleSignInDelegateImpl(activity: Activity):
    GoogleSignInDelegate<Pair<Boolean, Int>, Any, AccountEntity, TokenEntity>,
    CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.CACHED

    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        throw exception
    }

    private var activityRef = WeakReference<Activity>(activity)
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var gsiClientId: String

    init {
        launch(errorHandler) {
            InputStreamReader(activityRef.get()!!.resources?.openRawResource(R.raw.google_signin), StandardCharsets.UTF_8)
                .let { GsonBuilder().create().fromJson(it, GoogleSignInConfig::class.java) }
                .run { gsiClientId = clientId }
        }
    }

    override fun signIn(params: Pair<Boolean, Int>) {
        val isGoogleSignInUseIdToken = params.first
        val responseCode = params.second
        googleSignInClient = if (isGoogleSignInUseIdToken) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(gsiClientId)
                .requestEmail()
                .build()
            GoogleSignIn.getClient(activityRef.get()!!, gso)
        } else {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
            GoogleSignIn.getClient(activityRef.get()!!, gso)
        }
        val signInIntent = googleSignInClient.signInIntent
        activityRef.get()!!.startActivityForResult(signInIntent, responseCode)
    }

    override fun signOut(): TokenEntity {
        launch(errorHandler) {
            suspendCoroutine { continuation ->
                googleSignInClient.signOut()
                    .addOnCompleteListener { continuation.resume(Unit) }
            }
        }
        return TokenEntity.empty()
    }

    override fun handleAuthResponse(any: Any): AccountEntity {
        if (any !is Intent)
            throw IllegalArgumentException("Parameter 'any' should be an Intent")

        try {
            val completedTask = GoogleSignIn.getSignedInAccountFromIntent(any)
            val gsiAccount = completedTask.getResult(ApiException::class.java)
                ?: throw Exception("Empty Google SignIn Account")

            val tokenEntity = TokenEntity(gsiAccount.idToken?:"", gsiAccount.isExpired, 0L, "")
            val json = JSONObject(gsiAccount.zac())
            if (json.has("tokenId"))  json.remove("tokenId")
            val userInfo = prettyJsonObject(json)

            return AccountEntity(
                gsiAccount.displayName?:"N/A",
                gsiAccount.photoUrl?.toString()?:"",
                tokenEntity,
                userInfo)
        } catch (e: ApiException) {
            throw e
        }
    }

    override fun refreshToken(): TokenEntity {
        return rxSingle(errorHandler) {
            suspendCoroutine<GoogleSignInAccount> { continuation ->
                googleSignInClient.silentSignIn()
                    .addOnCompleteListener { task ->
                        task.result
                            ?.run {
                                continuation.resume(this)
                            }
                            ?:run {
                                continuation.resumeWithException(RecoverableException.CommonError(Throwable("Empty token")))
                            }
                    }
            }
        }
            .map { TokenEntity(it.idToken?:"", it.isExpired, 0L, "") }
            .toFuture()
            .get()
    }
}