package io.awesdroid.awesauthkt.presentation.common

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.awesdroid.awesauthkt.data.exception.AbstractException
import io.awesdroid.awesauthkt.data.exception.RecoverableException
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * @author Awesdroid
 */
abstract class BaseViewModel: ViewModel() {
    var error = MutableLiveData<AbstractException>()
    private val compositeDisposable = CompositeDisposable()

    protected fun handleError(error: Throwable) {
        when {
            error is AbstractException -> this.error.value = error
            error.cause is AbstractException -> this.error.value = error.cause as AbstractException
            else -> this.error.value = RecoverableException.CommonError(error)
        }
    }

    protected fun addDisposable(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }


    override fun onCleared() {
        compositeDisposable.clear()
    }
}