package io.awesdroid.awesauthkt.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import io.awesdroid.awesauthkt.di.DaggerRepositoryComponent
import io.awesdroid.awesauthkt.repository.SettingsRepository
import io.awesdroid.libkt.common.utils.TAG
import javax.inject.Inject

/**
 * @author Awesdroid
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val authType: LiveData<String>
    private val authTypeName: MediatorLiveData<String>
    @Inject lateinit var repository: SettingsRepository
    private val appAuthUsePendingIntent: LiveData<Boolean>
    private val googleSinInUseIdToken: LiveData<Boolean>

    init {
        DaggerRepositoryComponent.create().inject(this)
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