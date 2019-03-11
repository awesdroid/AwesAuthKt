package io.awesdroid.awesauthkt.presentation

import android.app.Application
import com.squareup.leakcanary.LeakCanary

/**
 * @author Awesdroid
 */
class App: Application() {

    override fun onCreate() {
        super.onCreate()
        initializeLeak()
    }

    private fun initializeLeak() {
        if (BuildConfig.DEBUG) LeakCanary.install(this)
    }
}