package io.awesdroid.awesauthkt.di

import android.content.Context
import dagger.Module
import dagger.Provides
import io.awesdroid.awesauthkt.utils.ActivityHelper

/**
 * @auther Awesdroid
 */
@Module
internal class ContextModule {
    @Provides
    fun provideContext(): Context {
        return ActivityHelper.getContext()
    }
}
