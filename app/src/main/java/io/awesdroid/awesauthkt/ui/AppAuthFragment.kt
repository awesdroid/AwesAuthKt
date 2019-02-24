package io.awesdroid.awesauthkt.ui

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
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import io.awesdroid.awesauthkt.R
import io.awesdroid.awesauthkt.model.AppAuthState
import io.awesdroid.awesauthkt.utils.RC_AUTH
import io.awesdroid.awesauthkt.utils.TYPE_APPAUTH
import io.awesdroid.awesauthkt.utils.TYPE_NONE
import io.awesdroid.awesauthkt.viewmodel.AppAuthViewModel
import io.awesdroid.awesauthkt.viewmodel.SettingsViewModel
import io.awesdroid.libkt.android.exceptions.LiveException
import io.awesdroid.libkt.common.utils.TAG
import io.awesdroid.libkt.common.utils.prettyJsonString
import kotlinx.android.synthetic.main.auth_status.*
import kotlinx.android.synthetic.main.fragment_appauth.*
import kotlinx.android.synthetic.main.fragment_appauth.view.*
import kotlinx.android.synthetic.main.user_info.*
import kotlinx.android.synthetic.main.user_info.view.*
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class AppAuthFragment : Fragment() {

    private val appAuthViewModel: AppAuthViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(AppAuthViewModel::class.java)
    }
    private var initViewModel = false
    private var authType: String? = null
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


        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        ViewModelProviders.of(requireActivity()).get(SettingsViewModel::class.java).let {
            it.getAuthType().observe(this, Observer { type -> this.handleAuthType(type) })
            it.isAppAuthUsePendingIntent().observe(this, Observer { ret -> this.handleUsePendingIntent(ret) })
        }

        appAuthViewModel.getError().observe(this, Observer { handleError(it) })
        appAuthViewModel.getAuthState().observe(this, Observer { this.handleAuthState(it) })
        appAuthViewModel.getUserInfo().observe(this, Observer { this.handleUserInfo(it) })
    }


    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        super.onDestroy()
    }


    private fun handleAuthType(type: String) {
        Log.d(TAG, "handleAuthType(): type = $type")
        authType = type
        if( type == TYPE_APPAUTH && !initViewModel ) {
            appAuthViewModel.init(requireActivity(), requireActivity())
            initViewModel = true
        }
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
        when (authType) {
            TYPE_NONE -> AlertDialog.Builder(requireActivity())
                .setMessage(getString(R.string.select_auth_type))
                .setPositiveButton(R.string.ok) { _, _ -> (activity as MainActivity).navigateToSettings() }
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
                .show()
            TYPE_APPAUTH -> {
                progressDialog.show()
                appAuthViewModel.signIn(usePendingIntent, RC_AUTH)
            }
            else -> throw IllegalStateException("authType is not 0")
        }
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

    private fun handleError(error: LiveException) {
        progressDialog.takeIf { it.isShowing }?.dismiss()
        alertDialog = AlertDialog.Builder(requireContext())
            .setTitle(error.type.name)
            .setMessage(error.exception.message)
            .setPositiveButton("OK") { _, _ ->
                alertDialog.dismiss()
                requireActivity().finish()
            }.show()
    }
}
