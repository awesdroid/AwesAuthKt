package io.awesdroid.awesauthkt.presentation.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import io.awesdroid.awesauthkt.presentation.R

/**
 * @author Awesdroid
 */
class SettingsFragment: PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }
}