package io.awesdroid.awesauthkt.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.awesdroid.awesauthkt.R
import io.awesdroid.awesauthkt.di.DaggerFragmentComponent
import io.awesdroid.awesauthkt.utils.*
import io.awesdroid.awesauthkt.viewmodel.AppAuthViewModel
import io.awesdroid.awesauthkt.viewmodel.SettingsViewModel
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var appAuthFragment: AppAuthFragment
    @Inject
    lateinit var settingsFragment: SettingsFragment
    @Inject
    lateinit var googleSignInFragment: GoogleSignInFragment

    private var fragmentManager: FragmentManager = supportFragmentManager
    private lateinit var currentAuthFragment: Fragment
    private var currentFragment = 0
    private lateinit var settingsViewModel: SettingsViewModel
    private var authTypeName: String = ""

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        currentFragment = item.itemId
        when (item.itemId) {
            R.id.navigation_auth -> {
                toolbar_text.text = authTypeName
                showAuthFragment(settingsViewModel.getAuthType().value ?: TYPE_APPAUTH)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_settings -> {
                toolbar_text.setText(R.string.title_settings)
                fragmentManager.beginTransaction().hide(currentAuthFragment).show(settingsFragment).commit()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    fun navigateToSettings() {
        navigation.selectedItemId = R.id.navigation_settings
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DaggerFragmentComponent.create().inject(this)

        val navigation: BottomNavigationView = findViewById(R.id.navigation)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        ActivityHelper.setActivity(this)

        currentAuthFragment = appAuthFragment

        settingsViewModel = ViewModelProviders.of(this).get(SettingsViewModel::class.java)
        settingsViewModel.getAuthTypeName().observe(this, Observer { this.setTitle(it) })

        fragmentManager.beginTransaction()
            .add(R.id.content, appAuthFragment, FRAGMENT_TAG_APPAUTH)
            .add(R.id.content, googleSignInFragment, FRAGMENT_TAG_GSI)
            .add(R.id.content, settingsFragment, FRAGMENT_TAG_SETTINGS)
            .hide(settingsFragment)
            .hide(appAuthFragment)
            .hide(googleSignInFragment)
            .commit()
        showAuthFragment(settingsViewModel.getAuthType().value)

    }

    private fun showAuthFragment(type: String?) {
        when(type) {
            TYPE_APPAUTH -> {
                fragmentManager.beginTransaction()
                    .hide(settingsFragment)
                    .hide(googleSignInFragment)
                    .show(appAuthFragment)
                    .commit()
                currentAuthFragment = appAuthFragment
            }
            TYPE_GSI -> {
                fragmentManager.beginTransaction()
                    .hide(settingsFragment)
                    .hide(appAuthFragment)
                    .show(googleSignInFragment)
                    .commit()
                currentAuthFragment = googleSignInFragment
            }
            else -> {
                AlertDialog.Builder(this)
                    .setMessage(getString(R.string.select_auth_type))
                    .setPositiveButton(R.string.ok) { _, _ -> this.navigateToSettings() }
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(true)
                    .show()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent(): $intent")
        setIntent(intent)
        handleAuthResponse(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult(): requestCode = $requestCode, data = $data")
        when(requestCode) {
            RC_AUTH -> handleAuthResponse(data)
            RC_SIGN_IN -> googleSignInFragment.onActivityResult(requestCode, resultCode, data)
            else -> return
        }

    }

    override fun onDestroy() {
        ActivityHelper.clear()
        super.onDestroy()
    }

    private fun setTitle(name: String) {
        authTypeName = name
        if (currentFragment != R.id.navigation_settings)
            toolbar_text.text = name
    }

    private fun handleAuthResponse(intent: Intent?) {
        intent?.let { ViewModelProviders.of(this).get(AppAuthViewModel::class.java).handleAuthResponse(it) }
    }
}
