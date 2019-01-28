package io.awesdroid.awesauthkt.di

import dagger.Component
import io.awesdroid.awesauthkt.ui.MainActivity

import javax.inject.Singleton

/**
 * @author Awesdroid
 */
@Singleton
@Component(modules = [FragmentModule::class])
interface FragmentComponent {
    fun inject(activity: MainActivity)
}
