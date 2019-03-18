package io.awesdroid.awesauthkt.presentation.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import io.awesdroid.awesauthkt.data.exception.AbstractException
import io.awesdroid.awesauthkt.data.exception.UnRecoverableException
import io.awesdroid.awesauthkt.domain.entity.AppAuthState
import io.awesdroid.awesauthkt.presentation.BuildConfig
import io.awesdroid.awesauthkt.presentation.R
import io.awesdroid.awesauthkt.presentation.common.BaseFragment
import io.awesdroid.awesauthkt.presentation.utils.RC_AUTH
import io.awesdroid.awesauthkt.presentation.viewmodel.AppAuthViewModel
import io.awesdroid.awesauthkt.presentation.viewmodel.SettingsViewModel
import io.awesdroid.libkt.common.utils.TAG
import io.awesdroid.libkt.common.utils.prettyJsonString
import kotlinx.android.synthetic.main.auth_status.*
import kotlinx.android.synthetic.main.fragment_appauth.*
import kotlinx.android.synthetic.main.fragment_appauth.view.*
import kotlinx.android.synthetic.main.user_info.*
import kotlinx.android.synthetic.main.user_info.view.*
import org.json.JSONException
import org.json.JSONObject
import org.kodein.di.generic.instance
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class AppAuthFragment: BaseFragment() {

    private val appAuthViewModel: AppAuthViewModel by instance()
    private val settingsViewModel: SettingsViewModel by instance()

    private var initViewModel = false
    private var usePendingIntent = false
    private lateinit var progressDialog: Dialog
    private lateinit var alertDialog: AlertDialog


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: ")
        val rootView = inflater.inflate(R.layout.fragment_appauth, container, false)

        val rv = inflater.inflate(R.layout.progress_bar, container, false)
        with(Dialog(requireActivity())) {
            progressDialog = this
            this
        }.apply {
            setContentView(rv)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        rootView?.button_signout?.visibility = View.GONE
        rootView?.button_refresh_token?.visibility = View.GONE
        rootView?.button_get_info?.visibility = View.GONE

        rootView?.button_signin?.setOnClickListener { signIn(usePendingIntent) }
        rootView?.button_signout?.setOnClickListener { signOut() }
        rootView?.button_get_info?.setOnClickListener { fetchUserInfo() }
        rootView?.button_refresh_token?.setOnClickListener { refreshToken() }

        rootView?.token_info_container?.visibility = View.GONE
        rootView?.userinfo_container?.visibility = View.GONE

        if(!initViewModel ) {
            appAuthViewModel.init(context!!, requireActivity(), requireActivity())
            initViewModel = true
        }

        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        settingsViewModel.isAppAuthUsePendingIntent()
            .observe(this, Observer { ret -> this.handleUsePendingIntent(ret) })

        appAuthViewModel.getError().observe(this, Observer { handleError(it) })
        appAuthViewModel.getAuthState().observe(this, Observer { this.handleAuthState(it) })
        appAuthViewModel.getUserInfo().observe(this, Observer { this.handleUserInfo(it) })
    }

    private fun handleUsePendingIntent(usePendingIntent: Boolean) {
        Log.d(TAG, "handleUsePendingIntent(): usePendingIntent = $usePendingIntent")
        this.usePendingIntent = usePendingIntent
    }

    private fun handleAuthState(state: AppAuthState?) {
        Log.d(TAG, "handleAuthState(): state = $state")
        state?: return

        when {
            state.hasLastTokenResponse -> {
                progressDialog.dismiss()

                // update UI
                auth_status.text = getString(R.string.auth_granted)
                auth_status.setTextColor(Color.WHITE)

                button_signin.visibility = View.GONE
                button_signout.visibility = View.VISIBLE
                button_refresh_token.visibility = View.VISIBLE
                button_refresh_token.isEnabled = state.refreshToken != null
                button_get_info.visibility = View.VISIBLE

                showTokenInfo(state)
            }
            state.authorizationCode != null -> {
                // Handle Authorization response
                auth_status.text = getString(R.string.fetching_token)
                auth_status.setTextColor(Color.WHITE)
                progressDialog.show()
            }
            else -> // handle exception
                state.authorizationException?.run {
                    AlertDialog.Builder(requireActivity())
                        .setMessage(this.message)
                        .setPositiveButton(R.string.ok) { _, _ ->  signOut() }
                        .setNegativeButton(R.string.cancel, null)
                        .setCancelable(true)
                        .show()
                }
        }
    }

    private fun signIn(usePendingIntent: Boolean) {
        progressDialog.show()
        appAuthViewModel.signIn(usePendingIntent, RC_AUTH)
    }


    @SuppressLint("SetTextI18n")
    private fun showTokenInfo(state: AppAuthState) {
        token_info_container.visibility = View.VISIBLE

        token_info_refresh.text = state.refreshToken?:"n/a"
        token_info_id.text = state.idToken?: "n/a"
        token_info_expire.text = when {
            state.accessToken == null -> "n/a"
            state.accessTokenExpirationTime == null -> "no expired time"
            (state.accessTokenExpirationTime as Long) < System.currentTimeMillis() -> "already expired"
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val ftf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss ZZ")
                    val expireString = ftf.format(
                        ZonedDateTime.ofInstant(
                            Instant.ofEpochMilli(state.accessTokenExpirationTime as Long), ZoneId.systemDefault()
                        )
                    )
                    "expired at $expireString"
                } else {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZ", Locale.US)
                    val date = Date(state.accessTokenExpirationTime as Long)
                    "expired at ${sdf.format(date)}"
                }
            }
        }
    }

    private fun handleUserInfo(userInfo: JSONObject?) {
        Log.d(TAG, "handleUserInfo(): userInfo = $userInfo")
        appAuthViewModel.getAuthState().value?: return

        progressDialog.dismiss()
        userInfo?:let {
            AlertDialog.Builder(requireActivity())
                .setMessage("No user info available!")
                .setPositiveButton(R.string.ok, null)
                .setCancelable(true)
                .show()
            return
        }

        userinfo_container.visibility = View.VISIBLE
        try {
            if (userInfo.has("name")) {
                userinfo_name.text = userInfo.getString("name")
            }
            if (userInfo.has("picture")) {
                val url = userInfo.getString("picture")
                Glide.with(requireActivity())
                    .load(url)
                    .apply(RequestOptions.bitmapTransform(CircleCrop()))
                    .into(userinfo_avatar)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        this.userinfo.text = prettyJsonString(userInfo.toString())

    }

    private fun signOut() {
        auth_status.text = getString(R.string.auth_not_granted)
        auth_status.setTextColor(Color.DKGRAY)

        button_signin.visibility = View.VISIBLE
        button_signout.visibility = View.GONE
        button_refresh_token.visibility = View.GONE
        button_get_info.visibility = View.GONE

        token_info_container.visibility = View.GONE

        userinfo_container.visibility = View.GONE
        userinfo.text = ""
        userinfo_name.text = ""

        appAuthViewModel.signOut()
    }

    private fun fetchUserInfo() {
        progressDialog.show()
        appAuthViewModel.fetchUserInfo()
    }

    private fun refreshToken() {
        progressDialog.show()
        appAuthViewModel.refreshToken()
    }

    private fun handleError(error: AbstractException) {
        progressDialog.takeIf { it.isShowing }?.dismiss()
        if (BuildConfig.DEBUG) error.printStackTrace()

        alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("${error::class.java.superclass?.simpleName}: ${error::class.java.simpleName}")
            .setMessage(error.reason?.message)
            .setPositiveButton("OK") { _, _ ->
                alertDialog.dismiss()
                if (error is UnRecoverableException)
                    requireActivity().finish()
            }.show()
    }
}
