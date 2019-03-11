package io.awesdroid.awesauthkt.presentation.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import io.awesdroid.awesauthkt.data.exception.AbstractException
import io.awesdroid.awesauthkt.domain.entity.AppAuthState
import io.awesdroid.awesauthkt.domain.interactors.AppAuthUseCase
import io.awesdroid.awesauthkt.presentation.common.BaseViewModel
import io.awesdroid.awesauthkt.presentation.ui.MainActivity
import io.awesdroid.libkt.common.utils.TAG
import org.json.JSONObject
import org.kodein.di.generic.instance

/**
 * @author Awesdroid
 */
class AppAuthViewModel : BaseViewModel() {
    private val appAuthUseCase: AppAuthUseCase by MainActivity.kodeinInstance.get()!!.instance()
    private val authState = MediatorLiveData<AppAuthState>()
    private val userInfo = MediatorLiveData<JSONObject>()

    override fun onCleared() {
        appAuthUseCase.clear.execute(Unit)
            .subscribe()
        super.onCleared()
    }

    fun init(context: Context, completeActivity: Activity, cancelActivity: Activity) {
        val disposable = appAuthUseCase.initState.execute(Triple(context, completeActivity, cancelActivity))
            .subscribe (
                { authState.value = it },
                { Log.w(TAG, "init(): No cached state $it") }
            )
        addDisposable(disposable)
    }

    fun handleAuthResponse(intent: Intent) {
        val disposable = appAuthUseCase.handleAuthResponse.execute(intent)
            .subscribe (
                { authState.value = it },
                { handleError(it) }
            )
        addDisposable(disposable)
    }

    fun signIn(usePendingIntent: Boolean, requestCode: Int) {
        val disposable = appAuthUseCase.signIn.execute(Pair(usePendingIntent, requestCode))
            .subscribe(
                {},
                { handleError(it) }
            )
        addDisposable(disposable)
    }

    fun signOut() {
        val disposable = appAuthUseCase.signOut.execute(Unit)
            .subscribe (
                { authState.value = it },
                { handleError(it) }
            )
        addDisposable(disposable)
    }

    fun fetchUserInfo() {
        val disposable = appAuthUseCase.getUerInfo.execute(Unit)
            .subscribe (
                { userInfo.value = JSONObject(it) },
                { handleError(it) }
            )
        addDisposable(disposable)
    }

    fun refreshToken() {
        val disposable = appAuthUseCase.refreshToken.execute(Unit)
            .subscribe (
                { authState.value = it },
                { handleError(it) }
            )
        addDisposable(disposable)
    }


    fun getAuthState(): LiveData<AppAuthState> {
        return authState
    }

    fun getUserInfo(): LiveData<JSONObject> {
        return userInfo
    }

    fun getError(): LiveData<AbstractException> {
        return error
    }
}
