package io.awesdroid.awesauthkt.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import io.awesdroid.awesauthkt.service.GoogleSignInService
import io.awesdroid.libkt.android.ui.ActivityHelper
import io.awesdroid.libkt.common.utils.TAG
import io.reactivex.schedulers.Schedulers

/**
 * @auther Awesdroid
 */
class GoogleSignInViewModel(application: Application) : AndroidViewModel(application) {
    private val account: MutableLiveData<GoogleSignInAccount> = MutableLiveData<GoogleSignInAccount>(null)
    private val googleSignInService: GoogleSignInService = GoogleSignInService()

    init {
        googleSignInService.init(ActivityHelper.getActivity()!!)
    }

    fun signIn(useIdToken: Boolean, responseCode: Int) {
        googleSignInService.doAuth(useIdToken, responseCode)
    }

    @SuppressLint("CheckResult")
    fun signOut() {
        googleSignInService.signOut()
            .observeOn(Schedulers.io())
            .subscribe { account.postValue(null) }
    }

    @SuppressLint("CheckResult")
    fun refreshToken() {
        googleSignInService.refreshToken()
            .observeOn(Schedulers.io())
            .subscribe { account.postValue(it) }
    }

    fun getAccount(): LiveData<GoogleSignInAccount> {
        return account
    }

    override fun onCleared() {
        googleSignInService.destroy()
        super.onCleared()
        Log.d(TAG, "onCleared: ")
    }
}
