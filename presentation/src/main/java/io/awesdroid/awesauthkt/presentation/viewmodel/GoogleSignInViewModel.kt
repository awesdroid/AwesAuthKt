package io.awesdroid.awesauthkt.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.awesdroid.awesauthkt.data.exception.AbstractException
import io.awesdroid.awesauthkt.domain.interactors.GoogleSignInUseCase
import io.awesdroid.awesauthkt.presentation.common.BaseViewModel
import io.awesdroid.awesauthkt.presentation.model.UserAccount
import io.awesdroid.awesauthkt.presentation.ui.MainActivity
import org.kodein.di.generic.instance

/**
 * @author Awesdroid
 */
class GoogleSignInViewModel : BaseViewModel() {
    private val googleSignIn: GoogleSignInUseCase by MainActivity.kodeinInstance.get()!!.kodein.instance()
    private val account = MutableLiveData<UserAccount>(null)

    fun signIn(useIdToken: Boolean, responseCode: Int) {
        val disposable = googleSignIn.signIn.execute(Pair(useIdToken, responseCode))
            .subscribe(
                {},
                { handleError(it) }
            )
        addDisposable(disposable)
    }

    fun signOut() {
        val disposable = googleSignIn.signOut.execute(Unit)
                .subscribe (
                    { account.postValue(null) },
                    { handleError(it) }
                )
        addDisposable(disposable)
    }

    fun refreshToken() {
        val disposable = googleSignIn.refreshToken.execute(Unit)
            .subscribe (
                { ret ->
                    val old = account.value?.copy() ?: UserAccount.empty()
                    val new = UserAccount(old.name, old.photoUrl, ret.idToken, ret.expiredTime, old.userInfo)
                    account.postValue(new) },
                { handleError(it) }
            )
        addDisposable(disposable)
    }

    fun handleAuthResponse(any: Any) {
        val disposable = googleSignIn.handleAuthResponse.execute(any)
            .map { UserAccount(it.name, it.photoUrl, it.tokenEntity.idToken, it.tokenEntity.expiredTime, it.userInfo) }
            .subscribe (
                { ret -> account.postValue(ret) },
                { handleError(it) }
            )
        addDisposable(disposable)
    }

    fun getAccount(): LiveData<UserAccount> {
        return account
    }

    fun getError(): LiveData<AbstractException> {
        return error
    }
}
