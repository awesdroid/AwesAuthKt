package io.awesdroid.awesauthkt.presentation.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.awesdroid.awesauthkt.data.di.DI
import io.awesdroid.awesauthkt.presentation.R
import io.awesdroid.awesauthkt.presentation.di.ACTIVITY_TAG
import io.awesdroid.awesauthkt.presentation.di.CONTEXT_TAG
import io.awesdroid.awesauthkt.presentation.di.mainKodeinModule
import io.awesdroid.awesauthkt.presentation.utils.RC_AUTH
import io.awesdroid.awesauthkt.presentation.utils.RC_SIGN_IN
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


    private val appAuthViewModel: AppAuthViewModel by kodein.instance()
    private val googleSignInViewModel: GoogleSignInViewModel by kodein.instance()

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        kodeinInstance = WeakReference(kodein)

        /* Inject kodein into data layer */
        DI.init(kodein)

        setNavController()

        setupBottomNavView()

        setupActionBar()
    }

    private fun setNavController() {
        val host: NavHostFragment = supportFragmentManager
            .findFragmentById(R.id.host_fragment) as NavHostFragment

        navController = host.navController
    }

    private fun setupBottomNavView() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        bottomNav.setupWithNavController(navController)
    }

    private fun setupActionBar() {
        setSupportActionBar(my_toolbar)
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.dest_home, R.id.dest_settings))
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent(): $intent")
        setIntent(intent)
        appAuthViewModel.handleAuthResponse(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult(): requestCode = $requestCode, data = $data")
        when(requestCode) {
            RC_AUTH -> data?.let { appAuthViewModel.handleAuthResponse(it) }
            RC_SIGN_IN -> data?.let { googleSignInViewModel.handleAuthResponse(data) }
            else -> return
        }
    }

    override fun onDestroy() {
        kodeinInstance.clear()
        DI.clear()
        super.onDestroy()
    }

    companion object {
        lateinit var kodeinInstance: WeakReference<Kodein>
    }
}
