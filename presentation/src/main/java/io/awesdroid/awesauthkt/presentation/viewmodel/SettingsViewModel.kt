package io.awesdroid.awesauthkt.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import io.awesdroid.awesauthkt.data.repository.SettingsRepository
import io.awesdroid.awesauthkt.presentation.common.BaseViewModel
import io.awesdroid.awesauthkt.presentation.ui.MainActivity
import io.awesdroid.libkt.common.utils.TAG
import org.kodein.di.generic.instance

/**
 * @author Awesdroid
 */
class SettingsViewModel : BaseViewModel() {
    private val repository: SettingsRepository by MainActivity.kodeinInstance.get()!!.instance()
    private val authType: LiveData<String>
    private val authTypeName: MediatorLiveData<String>
    private val appAuthUsePendingIntent: LiveData<Boolean>
    private val googleSinInUseIdToken: LiveData<Boolean>

    init {
        authType = repository.getAuthType()
        authTypeName = MediatorLiveData()
        authTypeName.addSource(repository.getAuthTypeName()) { authTypeName.postValue(it) }
        appAuthUsePendingIntent = repository.isAppAuthUsePendingIntent()
        googleSinInUseIdToken = repository.isGoogleSignInUseIdToken()
    }

    fun getAuthType(): LiveData<String> {
        return authType
    }

    fun getAuthTypeName(): LiveData<String> {
        return repository.getAuthTypeName()
    }

    fun setAuthTypeName(name: String) {
        repository.setAuthTypeName(name)
    }

    fun isAppAuthUsePendingIntent(): LiveData<Boolean> {
        return appAuthUsePendingIntent
    }

    fun isGoogleSignInUseIdToken(): LiveData<Boolean> {
        return googleSinInUseIdToken
    }

    override fun onCleared() {
        super.onCleared()
        repository.destroy()
        Log.d(TAG, "onCleared: ")
    }
}