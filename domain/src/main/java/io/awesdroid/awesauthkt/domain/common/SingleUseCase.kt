package io.awesdroid.awesauthkt.domain.common

import io.reactivex.Single

/**
 * @author Awesdroid
 */
abstract class SingleUseCase<Params, Result>(private val transformer: AbstractAsyncRxTransformer<Result>) {
    abstract fun process(params: Params): Result

    fun execute(params: Params): Single<Result> {
        return Single.create<Result> { emitter ->
            try {
                val result = process(params)
                emitter.onSuccess(result)
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }.compose(transformer)
    }
}