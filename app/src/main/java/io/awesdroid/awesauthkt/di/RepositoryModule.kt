package io.awesdroid.awesauthkt.di

import android.content.Context
import dagger.Module
import dagger.Provides
import io.awesdroid.awesauthkt.repository.AppAuthRepository
import io.awesdroid.awesauthkt.repository.SettingsRepository

import javax.inject.Singleton

/**
 * @author Awesdroid
 */
@Module
class RepositoryModule {
    @Singleton
    @Provides
    internal fun appAuthRepository(context: Context): AppAuthRepository {
        return AppAuthRepository(context)
    }

    @Singleton
    @Provides
    internal fun settingsRepository(context: Context): SettingsRepository {
        return SettingsRepository(context)
    }
}
