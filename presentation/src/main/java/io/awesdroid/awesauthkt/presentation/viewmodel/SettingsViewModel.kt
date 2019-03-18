package io.awesdroid.awesauthkt.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
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
    private val appAuthUsePendingIntent: LiveData<Boolean>
    private val googleSinInUseIdToken: LiveData<Boolean>

    init {
        appAuthUsePendingIntent = repository.isAppAuthUsePendingIntent()
        googleSinInUseIdToken = repository.isGoogleSignInUseIdToken()
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