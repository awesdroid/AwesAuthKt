package io.awesdroid.awesauthkt.service

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import io.awesdroid.awesauthkt.model.AppAuthConfig
import io.awesdroid.awesauthkt.net.Http
import io.awesdroid.libkt.android.ui.ActivityHelper
import io.awesdroid.libkt.common.utils.TAG
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import net.openid.appauth.*
import net.openid.appauth.AuthorizationServiceConfiguration.RetrieveConfigurationCallback
import net.openid.appauth.browser.AnyBrowserMatcher
import net.openid.appauth.connectivity.DefaultConnectionBuilder
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

/**
 * @auther Awesdroid
 */
class AppAuthService {
    private var context: Context? = null
    private var completeActivity: Activity? = null
    private var cancelActivity: Activity? = null
    private var isInit = false
    private var appAuthConfig: AppAuthConfig? = null
    private var authService: AuthorizationService? = null
    private var authRequest: AuthorizationRequest? = null
    private var authIntent: CustomTabsIntent? = null
    private var isWarmUpBrowser: CompletableFuture<Boolean>? = null
    private var authState: AuthState? = null

    fun init(context: Context, completeActivity: Activity, cancelActivity: Activity) {
        this.init(context, completeActivity, cancelActivity, null, null)
    }

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
        val discoveryUri = appAuthConfig.discovery_uri
        if (!discoveryUri?.toString().isNullOrEmpty())
            Single.just(discoveryUri)
            .subscribeOn(Schedulers.io())
            .subscribe { uri ->
                AuthorizationServiceConfiguration.fetchFromUrl(
                    uri!!,
                    RetrieveConfigurationCallback { config, ex -> handleConfigFetchResult(config,ex)},
                    DefaultConnectionBuilder.INSTANCE
                )
            }
        else
            Single.just(AuthorizationServiceConfiguration(appAuthConfig.authorization_endpoint_uri!!, appAuthConfig.token_endpoint_uri!!))
            .subscribe { configuration -> handleConfigFetchResult(configuration, null) }
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
        if (appAuthConfig?.client_id.isNullOrEmpty()) {
            Log.w(TAG, "initClient: dynamic registration is not supported yet")
            return
        }

        createAuthRequest(null)
        isWarmUpBrowser = warmUpBrowser()
    }

    private fun createAuthRequest(loginHint: String?) {
        Log.i(TAG, "Creating auth request for login hint: $loginHint")
        val authRequestBuilder = AuthorizationRequest.Builder(
            authState!!.authorizationServiceConfiguration!!,
            appAuthConfig!!.client_id!!,
            ResponseTypeValues.CODE,
            appAuthConfig!!.redirect_uri!!)
            .setScope(appAuthConfig!!.authorization_scope)

        if (!loginHint.isNullOrEmpty()) {
            authRequestBuilder.setLoginHint(loginHint)
        }
        authRequest = authRequestBuilder.build()
    }

    private fun warmUpBrowser(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            val intentBuilder = authService!!.createCustomTabsIntentBuilder(authRequest!!.toUri())
            authIntent = intentBuilder.build()
            true
        }
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
            AuthorizationService(ActivityHelper.getContext(), build())
        }
    }

    private fun resumeAppAuth() {
        recreateAuthorizationService()
        initClient()
    }

    fun doAuth(usePendingIntent: Boolean, requestCode: Int) {
        isWarmUpBrowser!!.thenRunAsync {
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
                    ActivityHelper.getActivity()!!.startActivityForResult(it, requestCode)
                }
            }
        }
    }

    fun handleAuthResponse(intent: Intent): Single<AuthState> {
        val response = AuthorizationResponse.fromIntent(intent)
        val exception = AuthorizationException.fromIntent(intent)

        if (response == null || exception != null) {
            return Single.create { emitter ->
                if (exception != null) {
                    emitter.onError(exception)
                } else {
                    emitter.onError(Throwable("Response is NULL!"))
                }
            }
        }
        authState!!.update(response, null)
        return Single.create { source ->
            performTokenRequest(response.createTokenExchangeRequest(),
                AuthorizationService.TokenResponseCallback { r, e ->
                    authState?.let {
                        it.update(r, e)
                        if (it.isAuthorized) {
                            source.onSuccess(it)
                        } else {
                            "Authorization Code exchange failed: ${e?.message}".also { msg ->
                                Log.w(TAG, msg)
                                source.onError(IllegalAccessException(msg))
                            }
                        }
                        return@TokenResponseCallback
                    }
                    source.onError(IllegalStateException("Current State is null and can't be updated"))
                }
            )
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
            return
        }
    }

    fun fetchUserInfo(): Maybe<JSONObject> {
        return Maybe.create { source ->
            authState!!.performActionWithFreshTokens(authService!!) { accessToken, _, ex ->
                if (ex != null) {
                    Log.e(TAG, "Token refresh failed when fetching user info")
                    source.onError(ex)
                    return@performActionWithFreshTokens
                }

                val discovery = authState!!.authorizationServiceConfiguration!!.discoveryDoc

                val userInfoEndpoint = if (appAuthConfig!!.user_info_endpoint_uri != null)
                    appAuthConfig!!.user_info_endpoint_uri!!.toString()
                else
                    discovery!!.userinfoEndpoint!!.toString()

                Http.getUserInfo(userInfoEndpoint, accessToken!!)
                    .subscribeOn(Schedulers.io())
                    .subscribe { info ->
                        Log.d(TAG, "fetchUserInfo(): info = $info")
                        if (info.isEmpty())
                            source.onComplete()
                        else {
                            try {
                                source.onSuccess(JSONObject(info))
                            } catch (e: JSONException) {
                                Log.w(TAG, "fetchUserInfo(): JSONException: ", e)
                                source.onError(e)
                            }

                        }
                    }
            }
        }
    }

    fun refreshAccessToken(): Single<AuthState> {
        return Single.create { source ->
            performTokenRequest(authState!!.createTokenRefreshRequest(),
                AuthorizationService.TokenResponseCallback { tokenResponse, exception ->
                    authState!!.update(tokenResponse, exception)
                    if (exception != null) {
                        source.onError(exception)
                    } else {
                        source.onSuccess(authState!!)
                    }
                })
        }

    }

    fun signOut(): Single<AuthState> {
        return Single.create { emitter ->
            when {
                authState == null ->
                    emitter.onError(IllegalArgumentException("authState is Null"))

                authState!!.authorizationServiceConfiguration == null ->
                    emitter.onError(IllegalArgumentException("authState has empty configuration"))

                else -> {
                    AuthState(authState!!.authorizationServiceConfiguration!!).let {
                        authState = it
                        emitter.onSuccess(it)
                    }
                }
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