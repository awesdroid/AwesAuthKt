package io.awesdroid.awesauthkt.presentation.common

import io.awesdroid.awesauthkt.domain.common.AbstractAsyncRxTransformer
import io.reactivex.Completable
import io.reactivex.CompletableSource
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


/**
 * @author Awesdroid
 */
class AsyncRxTransformer<T>: AbstractAsyncRxTransformer<T>() {
    override fun apply(upstream: Completable): CompletableSource {
        return upstream.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun apply(upstream: Single<T>): SingleSource<T> {
        return upstream.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }
}