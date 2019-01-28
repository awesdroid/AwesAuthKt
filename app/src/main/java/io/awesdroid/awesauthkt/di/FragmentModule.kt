package io.awesdroid.awesauthkt.di

import dagger.Module
import dagger.Provides
import io.awesdroid.awesauthkt.ui.AppAuthFragment
import io.awesdroid.awesauthkt.ui.GoogleSignInFragment
import io.awesdroid.awesauthkt.ui.SettingsFragment
import javax.inject.Singleton

/**
 * @author Awesdroid
 */
@Module
internal class FragmentModule {
    @Singleton
    @Provides
    fun appAuthFragment(): AppAuthFragment {
        return AppAuthFragment()
    }

    @Singleton
    @Provides
    fun googleSignInFragment(): GoogleSignInFragment {
        return GoogleSignInFragment()
    }

    @Singleton
    @Provides
    fun settingsFragment(): SettingsFragment {
        return SettingsFragment()
    }
}
