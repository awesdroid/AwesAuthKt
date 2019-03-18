package io.awesdroid.awesauthkt.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.awesdroid.awesauthkt.data.R
import io.awesdroid.libkt.common.utils.TAG

/**
 * @author Awesdroid
 */
class SettingsRepository(private var context: Context?) {
    private val appAuthUsePendingIntent: MutableLiveData<Boolean>
    private val googleSinInUseIdToken: MutableLiveData<Boolean>
    private val listener: SharedPreferences.OnSharedPreferenceChangeListener

    init {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        appAuthUsePendingIntent = MutableLiveData(
            sharedPreferences.getBoolean(
                context!!.resources.getString(R.string.pref_key_use_pending_intent), false
            )
        )
        googleSinInUseIdToken = MutableLiveData(
            sharedPreferences.getBoolean(
                context!!.resources.getString(R.string.pref_key_use_id_token), false
            )
        )

       listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            Log.d(TAG, "preferenceListener: key = $key")
           when (key) {
               context!!.resources.getString(R.string.pref_key_use_pending_intent) ->
                   appAuthUsePendingIntent.postValue(sp.getBoolean(key, false))
               context!!.resources.getString(R.string.pref_key_use_id_token) ->
                   googleSinInUseIdToken.postValue(sp.getBoolean(key, false))
               else -> Log.d(TAG, "onSharedPreferenceChanged(): Uncared key$key")
           }
        }

        PreferenceManager.getDefaultSharedPreferences(context)
            .registerOnSharedPreferenceChangeListener(listener)

    }

    fun isAppAuthUsePendingIntent(): LiveData<Boolean> {
        return appAuthUsePendingIntent
    }

    fun isGoogleSignInUseIdToken(): LiveData<Boolean> {
        return googleSinInUseIdToken
    }

    fun destroy() {
        PreferenceManager.getDefaultSharedPreferences(context)
            .unregisterOnSharedPreferenceChangeListener(listener)
        context = null
    }
}