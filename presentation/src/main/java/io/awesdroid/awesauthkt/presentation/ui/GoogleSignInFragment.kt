package io.awesdroid.awesauthkt.presentation.ui

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import io.awesdroid.awesauthkt.data.exception.AbstractException
import io.awesdroid.awesauthkt.data.exception.UnRecoverableException
import io.awesdroid.awesauthkt.presentation.BuildConfig
import io.awesdroid.awesauthkt.presentation.R
import io.awesdroid.awesauthkt.presentation.common.BaseFragment
import io.awesdroid.awesauthkt.presentation.di.ACTIVITY_TAG
import io.awesdroid.awesauthkt.presentation.model.UserAccount
import io.awesdroid.awesauthkt.presentation.utils.RC_SIGN_IN
import io.awesdroid.awesauthkt.presentation.viewmodel.GoogleSignInViewModel
import io.awesdroid.awesauthkt.presentation.viewmodel.SettingsViewModel
import io.awesdroid.libkt.common.utils.TAG
import kotlinx.android.synthetic.main.auth_status.*
import kotlinx.android.synthetic.main.fragment_google_signin.*
import kotlinx.android.synthetic.main.fragment_google_signin.view.*
import kotlinx.android.synthetic.main.user_info.*
import org.kodein.di.generic.instance

class GoogleSignInFragment : BaseFragment() {
    private var useIdToken = false
    private val googleSignInViewModel: GoogleSignInViewModel by instance()
    private val settingsViewModel: SettingsViewModel by instance()
    private lateinit var progressDialog: Dialog
    private lateinit var alertDialog: AlertDialog


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_google_signin, container, false)
        rootView?.button_signin?.setOnClickListener { signIn() }
        rootView?.button_signout?.setOnClickListener{ signOut() }
        rootView?.button_refresh_token?.setOnClickListener { refreshToken() }

        val rv = inflater.inflate(R.layout.progress_bar, container, false)
        Dialog(requireActivity()).apply {
            progressDialog = this
            setContentView(rv)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        googleSignInViewModel.getAccount()
            .observe(this, Observer { account ->
                Log.d(TAG, "onChanged(): account = $account")
                updateUI(account)
            })
        googleSignInViewModel.getError()
            .observe(this, Observer { handleError(it) })

        settingsViewModel.isGoogleSignInUseIdToken()
            .observe(this, Observer { useIdToken -> setUseIdToken(useIdToken) })
    }

    private fun updateUI(account: UserAccount?) {
        progressDialog.dismiss()
        if (account != null) {
            account.photoUrl.apply {
                if (isNotEmpty() || isNotBlank()) {
                    val activity by kodein.instance<Activity>(ACTIVITY_TAG)
                    Glide.with(activity)
                        .load(this)
                        .apply(RequestOptions.bitmapTransform(CircleCrop()))
                        .into(userinfo_avatar)
                }
            }

            auth_status.text = getString(R.string.auth_granted)
            auth_status.setTextColor(Color.WHITE)

            button_signin.visibility = View.GONE
            button_signout.visibility= View.VISIBLE
            button_refresh_token.visibility= View.VISIBLE
            button_refresh_token.isEnabled = useIdToken

            toke_info_container.visibility = View.VISIBLE
            token_info_expire.text = if (account.expiredTime > System.currentTimeMillis()) "true" else "false"
            token_info_id.text = if (useIdToken) account.idToken else "n/a"

            userinfo_container.visibility = View.VISIBLE
            userinfo_name.text = account.name
            userinfo_scrollview.isFocusableInTouchMode = true
            userinfo_scrollview.fullScroll(ScrollView.FOCUS_UP)
            userinfo.text = account.userInfo

        } else {
            auth_status.text = getString(R.string.auth_not_granted)
            auth_status.setTextColor(Color.DKGRAY)

            button_signin.visibility = View.VISIBLE
            button_signout.visibility = View.GONE
            button_refresh_token.visibility = View.GONE

            toke_info_container.visibility = View.GONE

            userinfo_name.text = ""
            userinfo_container.visibility = View.GONE
        }
    }

    private fun handleSignInResult(intent: Intent) {
        googleSignInViewModel.handleAuthResponse(intent)
    }

    private fun signIn() {
        progressDialog.show()
        googleSignInViewModel.signIn(useIdToken, RC_SIGN_IN)
    }

    private fun signOut() {
        progressDialog.show()
        googleSignInViewModel.signOut()
    }

    private fun refreshToken() {
        progressDialog.show()
        googleSignInViewModel.refreshToken()
    }

    private fun setUseIdToken(useIdToken: Boolean) {
        Log.d(TAG, "setUseIdToken(): useIdToken = $useIdToken")
        this.useIdToken = useIdToken
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
