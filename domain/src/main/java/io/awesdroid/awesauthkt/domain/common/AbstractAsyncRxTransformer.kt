package io.awesdroid.awesauthkt.domain.common

import io.reactivex.CompletableTransformer
import io.reactivex.SingleTransformer

/**
 * @author Awesdroid
 */
abstract class AbstractAsyncRxTransformer<T>: CompletableTransformer, SingleTransformer<T, T> {
}