package io.awesdroid.awesauthkt.presentation.ui

import android.app.Activity
import android.content.Context
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
import io.awesdroid.awesauthkt.data.TYPE_APPAUTH
import io.awesdroid.awesauthkt.data.TYPE_GSI
import io.awesdroid.awesauthkt.data.di.DI
import io.awesdroid.awesauthkt.presentation.R
import io.awesdroid.awesauthkt.presentation.di.ACTIVITY_TAG
import io.awesdroid.awesauthkt.presentation.di.CONTEXT_TAG
import io.awesdroid.awesauthkt.presentation.di.mainKodeinModule
import io.awesdroid.awesauthkt.presentation.utils.*
import io.awesdroid.awesauthkt.presentation.viewmodel.AppAuthViewModel
import io.awesdroid.awesauthkt.presentation.viewmodel.GoogleSignInViewModel
import io.awesdroid.awesauthkt.presentation.viewmodel.SettingsViewModel
import io.awesdroid.libkt.common.utils.TAG
import kotlinx.android.synthetic.main.activity_main.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity(), KodeinAware {

    override val kodein: Kodein = Kodein.lazy {
        bind<Context>(CONTEXT_TAG) with singleton { applicationContext }
        bind<Activity>(ACTIVITY_TAG) with singleton { this@MainActivity }
        bind<SettingsViewModel>() with singleton {
            ViewModelProviders.of(this@MainActivity).get(SettingsViewModel::class.java)
        }
        bind<AppAuthViewModel>() with singleton {
            ViewModelProviders.of(this@MainActivity).get(AppAuthViewModel::class.java)
        }
        bind<GoogleSignInViewModel>() with singleton {
            ViewModelProviders.of(this@MainActivity).get(GoogleSignInViewModel::class.java)
        }
        import(mainKodeinModule)
    }


    private val appAuthFragment: AppAuthFragment by kodein.instance()
    private val googleSignInFragment: GoogleSignInFragment by kodein.instance()
    private val settingsFragment: SettingsFragment by kodein.instance()
    private val settingsViewModel: SettingsViewModel by kodein.instance()
    private val appAuthViewModel: AppAuthViewModel by kodein.instance()

    private var fragmentManager: FragmentManager = supportFragmentManager
    private lateinit var currentAuthFragment: Fragment
    private var currentFragment = 0
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

        kodeinInstance = WeakReference(kodein)

        /* Inject kodein into data layer */
        DI.init(kodein)

        val navigation: BottomNavigationView = findViewById(R.id.navigation)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        currentAuthFragment = appAuthFragment

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
        kodeinInstance.clear()
        DI.clear()
        super.onDestroy()
    }

    private fun setTitle(name: String) {
        authTypeName = name
        if (currentFragment != R.id.navigation_settings)
            toolbar_text.text = name
    }

    private fun handleAuthResponse(intent: Intent?) {
        intent?.let { appAuthViewModel.handleAuthResponse(it) }
    }

    companion object {
        lateinit var kodeinInstance: WeakReference<Kodein>
    }
}
