package io.awesdroid.awesauthkt.domain.common

import io.reactivex.Completable

/**
 * @author Awesdroid
 */
abstract class CompletableUseCase<in Params>(private val transformer: AbstractAsyncRxTransformer<Any>) {
    abstract fun process(params: Params)

    fun execute(params: Params): Completable {
        return Completable.create { emitter ->
            try {
                process(params)
                emitter.onComplete()
            } catch (e: Throwable) {
                emitter.onError(e)
            }
        }.compose(transformer)
    }
}