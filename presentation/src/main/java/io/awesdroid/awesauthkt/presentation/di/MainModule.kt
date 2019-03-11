package io.awesdroid.awesauthkt.presentation.di

import io.awesdroid.awesauthkt.data.delegate.AppAuthDelegateImpl
import io.awesdroid.awesauthkt.data.delegate.GoogleSignInDelegateImpl
import io.awesdroid.awesauthkt.data.di.DI_ACTIVITY_TAG
import io.awesdroid.awesauthkt.data.di.DI_CONTEXT_TAG
import io.awesdroid.awesauthkt.data.repository.SettingsRepository
import io.awesdroid.awesauthkt.domain.common.AbstractAsyncRxTransformer
import io.awesdroid.awesauthkt.domain.delegate.AppAuthDelegate
import io.awesdroid.awesauthkt.domain.delegate.GoogleSignInDelegate
import io.awesdroid.awesauthkt.domain.entity.AccountEntity
import io.awesdroid.awesauthkt.domain.entity.AppAuthState
import io.awesdroid.awesauthkt.domain.entity.TokenEntity
import io.awesdroid.awesauthkt.domain.interactors.AppAuthUseCase
import io.awesdroid.awesauthkt.domain.interactors.GoogleSignInUseCase
import io.awesdroid.awesauthkt.presentation.common.AsyncRxTransformer
import io.awesdroid.awesauthkt.presentation.ui.AppAuthFragment
import io.awesdroid.awesauthkt.presentation.ui.GoogleSignInFragment
import io.awesdroid.awesauthkt.presentation.ui.SettingsFragment
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

const val CONTEXT_TAG = DI_CONTEXT_TAG
const val ACTIVITY_TAG = DI_ACTIVITY_TAG


val mainKodeinModule = Kodein.Module("MainModule") {
    bind<AppAuthFragment>() with singleton { AppAuthFragment() }
    bind<GoogleSignInFragment>() with singleton { GoogleSignInFragment() }
    bind<SettingsFragment>() with singleton { SettingsFragment() }

    bind<SettingsRepository>() with singleton {
        SettingsRepository(instance(CONTEXT_TAG))
    }
    bind<AbstractAsyncRxTransformer<Any>>() with singleton { AsyncRxTransformer<Any>() }

    bind<AppAuthDelegate<Triple<Any, Any, Any>, Pair<Boolean, Int>, Any, AppAuthState, AppAuthState, String>>() with
            singleton { AppAuthDelegateImpl() }
    bind<AppAuthUseCase>() with singleton { AppAuthUseCase(instance(), instance()) }

    bind<GoogleSignInDelegate<Pair<Boolean, Int>, Any, AccountEntity, TokenEntity>>() with singleton {
        GoogleSignInDelegateImpl(instance(ACTIVITY_TAG))
    }
    bind<GoogleSignInUseCase>() with singleton { GoogleSignInUseCase(instance(), instance()) }
}