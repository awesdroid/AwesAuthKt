package io.awesdroid.awesauthkt.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import io.awesdroid.awesauthkt.di.DaggerRepositoryComponent
import io.awesdroid.awesauthkt.model.AppAuthState
import io.awesdroid.awesauthkt.repository.AppAuthRepository
import io.awesdroid.libkt.android.exceptions.LiveException
import io.awesdroid.libkt.common.utils.TAG
import org.json.JSONObject
import javax.inject.Inject

/**
 * @author Awesdroid
 */
class AppAuthViewModel(application: Application) : AndroidViewModel(application) {
    @Inject
    lateinit var repository: AppAuthRepository
    private val authState = MediatorLiveData<AppAuthState>()
    private val userInfo = MediatorLiveData<JSONObject>()

    init {
        DaggerRepositoryComponent.create().inject(this)
        authState.addSource(repository.getAppAuthState()) { authState.postValue(it) }
        userInfo.addSource(repository.getUserInfo()) { userInfo.postValue(it) }
    }

    fun init(completeActivity: Activity, cancelActivity: Activity) {
        repository.init(completeActivity, cancelActivity)
    }

    fun handleAuthResponse(intent: Intent) {
        repository.handleAuthResponse(intent)
    }

    fun signIn(usePendingIntent: Boolean, requestCode: Int) {
        repository.signIn(usePendingIntent, requestCode)
    }

    fun fetchUserInfo() {
        repository.fetchUserInfo()
    }

    fun refreshToken() {
        repository.refreshToken()
    }

    fun signOut() {
        repository.signOut()
    }

    fun getAuthState(): LiveData<AppAuthState> {
        return authState
    }

    fun getUserInfo(): LiveData<JSONObject> {
        return userInfo
    }

    fun getError(): LiveData<LiveException> {
        return repository.getError()
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared: ")
        repository.destroy()
        super.onCleared()
    }
}
