package io.awesdroid.awesauthkt.domain.interactors

import io.awesdroid.awesauthkt.domain.common.AbstractAsyncRxTransformer
import io.awesdroid.awesauthkt.domain.common.CompletableUseCase
import io.awesdroid.awesauthkt.domain.common.SingleUseCase
import io.awesdroid.awesauthkt.domain.delegate.AppAuthDelegate
import io.awesdroid.awesauthkt.domain.entity.AppAuthState

/**
 * @author Awesdroid
 */

class AppAuthUseCase(
    private val delegate: AppAuthDelegate<Triple<Any, Any, Any>, Pair<Boolean, Int>, Any, AppAuthState, AppAuthState, String>,
    private val transformer: AbstractAsyncRxTransformer<Any>
) {
    val initState = InitState()
    val signIn = SignIn()
    val signOut = SignOut()
    val handleAuthResponse = HandleAuthResponse()
    val refreshToken = RefreshToken()
    val getUerInfo = GetUerInfo()
    val clear = Clear()

    inner class InitState: SingleUseCase<Triple<Any, Any, Any>, AppAuthState>(transformer as AbstractAsyncRxTransformer<AppAuthState>) {
        override fun process(params: Triple<Any, Any, Any>): AppAuthState {
            return delegate.initState(params)
        }
    }

    inner class SignIn: CompletableUseCase<Pair<Boolean, Int>>(transformer) {
        override fun process(params: Pair<Boolean, Int>) {
            delegate.signIn(params)
        }
    }

    inner class HandleAuthResponse: SingleUseCase<Any, AppAuthState>(transformer as AbstractAsyncRxTransformer<AppAuthState>) {
        override fun process(params: Any): AppAuthState {
            return delegate.handleAuthResponse(params)
        }
    }

    inner class SignOut: SingleUseCase<Unit, AppAuthState>(transformer as AbstractAsyncRxTransformer<AppAuthState>) {
        override fun process(params: Unit): AppAuthState {
            return delegate.signOut()
        }
    }

    inner class RefreshToken: SingleUseCase<Unit, AppAuthState>(transformer as AbstractAsyncRxTransformer<AppAuthState>) {
        override fun process(params: Unit): AppAuthState {
            return delegate.refreshToken()
        }
    }

    inner class GetUerInfo: SingleUseCase<Unit, String>(transformer as AbstractAsyncRxTransformer<String>) {
        override fun process(params: Unit): String {
            return delegate.getUserInfo()
        }
    }

    inner class Clear: CompletableUseCase<Unit>(transformer) {
        override fun process(params: Unit) {
            return delegate.clear()
        }
    }
}