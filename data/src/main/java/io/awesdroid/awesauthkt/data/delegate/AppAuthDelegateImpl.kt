package io.awesdroid.awesauthkt.data.delegate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import io.awesdroid.awesauthkt.data.extension.toAppAuthState
import io.awesdroid.awesauthkt.data.model.AppAuthConfig
import io.awesdroid.awesauthkt.data.repository.AppAuthRepository
import io.awesdroid.awesauthkt.data.repository.AuthRepository
import io.awesdroid.awesauthkt.domain.delegate.AppAuthDelegate
import io.awesdroid.awesauthkt.domain.entity.AppAuthState
import io.awesdroid.libkt.common.executors.Dispatchers
import io.awesdroid.libkt.common.utils.TAG
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.openid.appauth.AuthState
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

/**
 * @author Awesdroid
 */
class AppAuthDelegateImpl:
    AppAuthDelegate<Triple<Any, Any, Any>, Pair<Boolean, Int>, Any, AppAuthState, AppAuthState, String>,
    CoroutineScope
{
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.CACHED

    private val authRepository: AuthRepository<AppAuthConfig, AuthState> by lazy { AppAuthRepository(contextRef.get()!!)}

    private val appAuthService = AppAuthService()
    private lateinit var contextRef: WeakReference<Context>
    private lateinit var completeActivityRef: WeakReference<Activity>
    private lateinit var cancelActivityRef: WeakReference<Activity>

    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        Log.d(TAG, "errorHandler(): e = $exception")
        throw exception
    }


    override fun initState(params: Triple<Any, Any, Any>): AppAuthState {
        if (params.first !is Context || params.second !is Activity || params.third !is Activity)
            throw IllegalArgumentException("Wrong parameters type $params")

        contextRef = WeakReference(params.first as Context)
        completeActivityRef = WeakReference(params.second as Activity)
        cancelActivityRef = WeakReference(params.third as Activity)

        val config = authRepository.loadAuthConfiguration()

        val authState = authRepository.loadAuthState()
        Log.d(TAG, "init(): authState = $authState")

        appAuthService.init(contextRef.get()!!, completeActivityRef.get()!!, cancelActivityRef.get()!!, config, authState)
        return authState?.let { it.toAppAuthState()} ?: AppAuthState.empty()
    }

    override fun signIn(params: Pair<Boolean, Int>) {
        launch(errorHandler) {
            appAuthService.doAuth(params.first, params.second, completeActivityRef.get()!!, cancelActivityRef.get()!!)
        }
    }

    override fun handleAuthResponse(response: Any): AppAuthState {
        if (response !is Intent)
            throw IllegalArgumentException("Parameter `response` should be an Intent")

        return appAuthService.handleAuthResponse(response).toFuture().get()
            .let {
                authRepository.storeAuthState(it)
                it.toAppAuthState()
            }
    }

    override fun signOut(): AppAuthState {
        return appAuthService.signOut().toFuture().get().let {
            authRepository.storeAuthState(it)
            it.toAppAuthState()
        }
    }

    override fun refreshToken(): AppAuthState {
        return appAuthService.refreshAccessToken().toFuture().get().let {
            authRepository.storeAuthState(it)
            it.toAppAuthState()
        }
    }

    override fun getUserInfo(): String {
        return appAuthService.fetchUserInfo().toSingle().toFuture().get().toString()
    }

    override fun clear() {
        appAuthService.destroy()
        authRepository.clean()
    }
}