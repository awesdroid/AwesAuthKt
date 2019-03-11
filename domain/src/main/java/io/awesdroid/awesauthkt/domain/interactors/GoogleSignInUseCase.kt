package io.awesdroid.awesauthkt.domain.interactors

import io.awesdroid.awesauthkt.domain.common.AbstractAsyncRxTransformer
import io.awesdroid.awesauthkt.domain.common.CompletableUseCase
import io.awesdroid.awesauthkt.domain.common.SingleUseCase
import io.awesdroid.awesauthkt.domain.delegate.GoogleSignInDelegate
import io.awesdroid.awesauthkt.domain.entity.AccountEntity
import io.awesdroid.awesauthkt.domain.entity.TokenEntity

/**
 * @author Awesdroid
 */

class GoogleSignInUseCase(private val delegate: GoogleSignInDelegate<Pair<Boolean, Int>, Any, AccountEntity, TokenEntity>,
                          private val transformer: AbstractAsyncRxTransformer<Any>
) {
    val signIn = SignIn()
    val handleAuthResponse = HandleAuthResponse()
    val signOut = SignOut()
    val refreshToken = RefreshToken()

    inner class SignIn: CompletableUseCase<Pair<Boolean, Int>>(transformer) {
        override fun process(params: Pair<Boolean, Int>) {
            delegate.signIn(params)
        }
    }

    inner class HandleAuthResponse: SingleUseCase<Any, AccountEntity>(transformer as AbstractAsyncRxTransformer<AccountEntity>) {
        override fun process(params: Any): AccountEntity {
            return delegate.handleAuthResponse(params)
        }
    }

    inner class SignOut: CompletableUseCase<Unit>(transformer) {
        override fun process(params: Unit) {
            delegate.signOut()
        }
    }

    inner class RefreshToken: SingleUseCase<Unit, TokenEntity>(transformer as AbstractAsyncRxTransformer<TokenEntity>) {
        override fun process(params: Unit): TokenEntity {
            return delegate.refreshToken()
        }
    }
}