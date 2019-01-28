package io.awesdroid.awesauthkt.di

import dagger.Component
import io.awesdroid.awesauthkt.viewmodel.AppAuthViewModel
import io.awesdroid.awesauthkt.viewmodel.SettingsViewModel

import javax.inject.Singleton

/**
 * @author Awesdroid
 */
@Singleton
@Component(modules = [RepositoryModule::class, ContextModule::class])
interface RepositoryComponent {
    fun inject(viewModel: AppAuthViewModel)
    fun inject(viewModel: SettingsViewModel)
}
