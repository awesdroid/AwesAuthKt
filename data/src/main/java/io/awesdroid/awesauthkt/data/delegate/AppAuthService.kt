package io.awesdroid.awesauthkt.data.delegate

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import io.awesdroid.awesauthkt.data.model.AppAuthConfig
import io.awesdroid.awesauthkt.data.net.Http
import io.awesdroid.libkt.common.executors.Dispatchers.CACHED
import io.awesdroid.libkt.common.utils.TAG
import io.reactivex.Maybe
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.rx2.rxMaybe
import kotlinx.coroutines.rx2.rxSingle
import net.openid.appauth.*
import net.openid.appauth.AuthorizationServiceConfiguration.RetrieveConfigurationCallback
import net.openid.appauth.browser.AnyBrowserMatcher
import net.openid.appauth.connectivity.DefaultConnectionBuilder
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * @author Awesdroid
 */
class AppAuthService: CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = CACHED

    private var context: Context? = null
    private var completeActivity: Activity? = null
    private var cancelActivity: Activity? = null
    private var isInit = false
    private var appAuthConfig: AppAuthConfig? = null
    private var authService: AuthorizationService? = null
    private var authRequest: AuthorizationRequest? = null
    private var authIntent: CustomTabsIntent? = null
    private lateinit var isWarmUpBrowser: Deferred<Boolean>
    private var authState: AuthState? = null


    fun init(context: Context, completeActivity: Activity, cancelActivity: Activity,
                     config: AppAuthConfig?, initState: AuthState?) {
        if (isInit) return
        if (config == null)
            throw IllegalArgumentException("AppAuthConfig is null")

        this.context = context
        this.completeActivity = completeActivity
        this.cancelActivity = cancelActivity
        this.appAuthConfig = config
        this.authState = initState

        if (authState?.authorizationServiceConfiguration != null) {
            resumeAppAuth()
        } else {
            initAppAuth(appAuthConfig!!)
        }

        isInit = true
    }

    private fun initAppAuth(appAuthConfig: AppAuthConfig) {
        this.appAuthConfig = appAuthConfig
        val discoveryUri = appAuthConfig.discoveryUri
        if (!discoveryUri?.toString().isNullOrEmpty())
            AuthorizationServiceConfiguration.fetchFromUrl(
                discoveryUri!!,
                RetrieveConfigurationCallback { config, ex -> handleConfigFetchResult(config,ex)},
                DefaultConnectionBuilder.INSTANCE
            )
        else
            AuthorizationServiceConfiguration(
                appAuthConfig.authorizationEndpointUri!!,
                appAuthConfig.tokenEndpointUri!!
            )
            .run { handleConfigFetchResult(this, null) }
    }

    private fun handleConfigFetchResult(
        config: AuthorizationServiceConfiguration?,
        ex: AuthorizationException?) {
        if (config == null) {
            Log.i(TAG, "Failed to retrieve discovery document", ex)
            return
        }

        authState = AuthState(config)
        Log.i(TAG, "Fetched discovery configuration is " + config.toJsonString())
        resumeAppAuth()
    }

    private fun initClient() {
        if (appAuthConfig?.clientId.isNullOrEmpty()) {
            Log.w(TAG, "initClient: dynamic registration is not supported yet")
            return
        }

        createAuthRequest(null)
        isWarmUpBrowser = warmUpBrowserAsync()
    }

    private fun createAuthRequest(loginHint: String?) {
        Log.i(TAG, "Creating auth request for login hint: $loginHint")
        val authRequestBuilder = AuthorizationRequest.Builder(
            authState!!.authorizationServiceConfiguration!!,
            appAuthConfig!!.clientId!!,
            ResponseTypeValues.CODE,
            appAuthConfig!!.redirectUri!!)
            .setScope(appAuthConfig!!.authorizationScope)

        if (!loginHint.isNullOrEmpty()) {
            authRequestBuilder.setLoginHint(loginHint)
        }
        authRequest = authRequestBuilder.build()
    }
    private fun warmUpBrowserAsync() = async {
        val intentBuilder = authService!!.createCustomTabsIntentBuilder(authRequest!!.toUri())
        authIntent = intentBuilder.build()
        true
    }

    private fun recreateAuthorizationService() {
        Log.d(TAG, "recreateAuthorizationService: authService = $authService")

        authService?.run {
            Log.i(TAG, "Discarding existing AuthService instance")
            dispose()
        }
        authService = createAuthorizationService()
        authRequest = null
        authIntent = null
    }

    private fun createAuthorizationService(): AuthorizationService {
        Log.i(TAG, "createAuthorizationService(): ")
        return AppAuthConfiguration.Builder().run {
            setBrowserMatcher(AnyBrowserMatcher.INSTANCE)
            setConnectionBuilder(DefaultConnectionBuilder.INSTANCE)
            AuthorizationService(context!!, build())
        }
    }

    private fun resumeAppAuth() {
        Log.w(TAG, "resumeAppAuth(): ")
        recreateAuthorizationService()
        initClient()
    }

    suspend fun doAuth(usePendingIntent: Boolean, requestCode: Int, completeActivity: Activity, cancelActivity: Activity) {
        if(isWarmUpBrowser.await()) {
            if (usePendingIntent) {
                Log.i(TAG, "doAuth(): usePendingIntent")
                val completionIntent = Intent(context, completeActivity!!.javaClass)
                val cancelIntent = Intent(context, cancelActivity!!.javaClass).apply {
                    putExtra("failed", true)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                authService!!.performAuthorizationRequest(
                    authRequest!!,
                    PendingIntent.getActivity(context, 0, completionIntent, 0),
                    PendingIntent.getActivity(context, 0, cancelIntent, 0),
                    authIntent!!)
            } else {
                Log.i(TAG, "doAuth(): NOT usePendingIntent")
                authService!!.getAuthorizationRequestIntent(authRequest!!, authIntent!!).let {
                    completeActivity.startActivityForResult(it, requestCode)
                }
            }
        } else {
            throw Exception("Browser is not warm up!")
        }
    }

    fun handleAuthResponse(intent: Intent): Single<AuthState> {
        val response = AuthorizationResponse.fromIntent(intent)
        val exception = AuthorizationException.fromIntent(intent)

        if (response == null || exception != null) {
            return rxSingle {
                if (exception != null) {
                    throw exception
                } else {
                    throw Throwable("Response is NULL!")
                }
            }
        }
        authState!!.update(response!!, null)
        return rxSingle {
            suspendCoroutine<AuthState> { continuation ->
                performTokenRequest(response.createTokenExchangeRequest(),
                    AuthorizationService.TokenResponseCallback { r, e ->
                        authState?.let {
                            it.update(r, e)
                            if (it.isAuthorized) {
                                continuation.resume(it)
                            } else {
                                "Authorization Code exchange failed: ${e?.message}".also { msg ->
                                    Log.w(TAG, msg)
                                    continuation.resumeWithException(IllegalAccessException(msg))
                                }
                            }
                            return@TokenResponseCallback
                        }
                        continuation.resumeWithException(IllegalStateException("Current State is null and can't be updated"))
                    }
                )
            }
        }
    }


    private fun performTokenRequest(
        request: TokenRequest,
        callback: AuthorizationService.TokenResponseCallback) {
        try {
            authState!!.clientAuthentication.run {
                authService!!.performTokenRequest(request,this, callback)
            }
        } catch (ex: ClientAuthentication.UnsupportedAuthenticationMethod) {
            Log.d(TAG, "Token request cannot be made, client authentication for the token endpoint could not be constructed $ex")
            throw ex
        }
    }

    fun fetchUserInfo(): Maybe<JSONObject> {
        return rxMaybe {
            Log.i(TAG, "async start: ====== ")
            if (authState == null || authService == null)
                throw Throwable("authState is Null")

            val accessToken = suspendCoroutine<String> { continuation ->
                authState!!.performActionWithFreshTokens(authService!!) { token, _, ex ->
                    ex?.
                        run { continuation.resumeWithException(ex) }
                        ?: token?.
                            let { continuation.resume(it) }
                            ?: let { continuation.resume("") }
                }
            }

            if (accessToken.isEmpty() || appAuthConfig == null)
                return@rxMaybe null

            val userInfoEndpoint =
                appAuthConfig!!.userInfoEndpointUri
                    ?.toString()
                    ?: authState?.authorizationServiceConfiguration?.discoveryDoc?.userinfoEndpoint?.toString()
                    ?:""


            try {
                val userInfo = Http.getUserInfo(userInfoEndpoint, accessToken)
                if (userInfo == null || userInfo.isEmpty())
                    return@rxMaybe null
                return@rxMaybe JSONObject(userInfo)
            } catch (e: Throwable) {
                throw e
            }
        }
    }

    fun refreshAccessToken(): Single<AuthState> {
        return rxSingle {
            suspendCoroutine<AuthState> { continuation ->
                performTokenRequest(
                    authState!!.createTokenRefreshRequest(),
                    AuthorizationService.TokenResponseCallback { tokenResponse, exception ->
                        authState!!.update(tokenResponse, exception)
                        if (exception != null) {
                            continuation.resumeWithException(exception)
                        } else {
                            continuation.resume(authState!!)
                        }
                    }
                )
            }
        }
    }

    fun signOut(): Single<AuthState> {
        return rxSingle {
            when {
                authState == null ->
                    throw IllegalArgumentException("authState is Null")

                authState!!.authorizationServiceConfiguration == null ->
                    throw IllegalArgumentException("authState has empty configuration")

                else -> AuthState(authState!!.authorizationServiceConfiguration!!).also { authState = it }
            }
        }
    }

    fun destroy() {
        Log.d(TAG, "destroy(): authService = $authService")
        authService?.dispose()
        authService = null
        context = null
        completeActivity = null
        cancelActivity = null
        isInit = false
    }
}