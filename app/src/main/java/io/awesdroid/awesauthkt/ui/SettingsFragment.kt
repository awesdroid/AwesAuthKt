package io.awesdroid.awesauthkt.ui

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import io.awesdroid.awesauthkt.R
import io.awesdroid.awesauthkt.utils.TYPE_APPAUTH
import io.awesdroid.awesauthkt.utils.TYPE_NONE
import io.awesdroid.awesauthkt.viewmodel.SettingsViewModel
import io.awesdroid.libkt.common.utils.TAG

/**
 * @author Awesdroid
 */
class SettingsFragment: PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val key = requireActivity().resources.getString(R.string.pref_key_google_auth_type)
        setPreferencesFromResource(R.xml.settings, rootKey)
        val listPreference = findPreference<ListPreference>(key)
        listPreference.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        val viewModel = ViewModelProviders.of(requireActivity()).get(SettingsViewModel::class.java)

        viewModel.getAuthType().observe(this, Observer {
            Log.d(TAG, "onCreatePreferences(): it = $it")
            val usePendingIntentPref =
                findPreference<CheckBoxPreference>(getString(R.string.pref_key_use_pending_intent))
            val useIdTokenPref = findPreference<SwitchPreference>(getString(R.string.pref_key_use_id_token))
            when(it) {
                TYPE_NONE -> {
                    usePendingIntentPref.isEnabled = false
                    useIdTokenPref.isEnabled = false
                }
                TYPE_APPAUTH -> {
                    viewModel.setAuthTypeName(listPreference.entry.toString())
                    usePendingIntentPref.isEnabled = true
                    useIdTokenPref.isEnabled = false
                }
                else -> {
                    viewModel.setAuthTypeName(listPreference.entry.toString())
                    usePendingIntentPref.isEnabled = false
                    useIdTokenPref.isEnabled = true
                }

            }
        })
        Log.d(TAG, "onCreatePreferences(): ")
    }
}