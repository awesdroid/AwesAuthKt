package io.awesdroid.awesauthkt.ui

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
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import io.awesdroid.awesauthkt.R
import io.awesdroid.awesauthkt.utils.RC_SIGN_IN
import io.awesdroid.awesauthkt.utils.TYPE_GSI
import io.awesdroid.awesauthkt.utils.TYPE_NONE
import io.awesdroid.awesauthkt.viewmodel.GoogleSignInViewModel
import io.awesdroid.awesauthkt.viewmodel.SettingsViewModel
import io.awesdroid.libkt.common.utils.TAG
import io.awesdroid.libkt.common.utils.prettyJsonObject
import kotlinx.android.synthetic.main.auth_status.*
import kotlinx.android.synthetic.main.fragment_google_signin.*
import kotlinx.android.synthetic.main.fragment_google_signin.view.*
import kotlinx.android.synthetic.main.user_info.*
import org.json.JSONObject

class GoogleSignInFragment : Fragment() {
    private var useIdToken = false
    private var authType = TYPE_NONE

    private lateinit var googleSignInViewModel: GoogleSignInViewModel
    private lateinit var progressDialog: Dialog


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: ")
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
        googleSignInViewModel = ViewModelProviders.of(requireActivity()).get(GoogleSignInViewModel::class.java)
        googleSignInViewModel.getAccount().observe(requireActivity(), Observer { account ->
            Log.d(TAG, "onChanged(): account = $account")
            updateUI(account)
        })

        ViewModelProviders.of(requireActivity()).get(SettingsViewModel::class.java).let {
            it.getAuthType().observe(requireActivity(), Observer { type -> this.setAuthType(type) })
            it.isGoogleSignInUseIdToken().observe(requireActivity(), Observer { useIdToken -> this.setUseIdToken(useIdToken) })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult(): resultCode = $requestCode")

        if (requestCode == RC_SIGN_IN) {
            handleSignInResult(GoogleSignIn.getSignedInAccountFromIntent(data))
        }
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        Log.d(TAG, "updateUI(): account = $account")
        progressDialog.dismiss()
        if (account != null) {
            account.photoUrl?.let {
                Glide.with(requireActivity())
                    .load(it)
                    .apply(RequestOptions.bitmapTransform(CircleCrop()))
                    .into(userinfo_avatar)
            }

            auth_status.text = getString(R.string.auth_granted)
            auth_status.setTextColor(Color.WHITE)

            button_signin.visibility = View.GONE
            button_signout.visibility= View.VISIBLE
            button_refresh_token.visibility= View.VISIBLE
            button_refresh_token.isEnabled = useIdToken

            toke_info_container.visibility = View.VISIBLE
            token_info_expire.text = if (account.isExpired) "true" else "false"
            token_info_id.text = if (useIdToken) account.idToken else "n/a"

            userinfo_container.visibility = View.VISIBLE
            userinfo_name.text = account.displayName
            userinfo_scrollview.isFocusableInTouchMode = true
            userinfo_scrollview.fullScroll(ScrollView.FOCUS_UP)
            userinfo.text = buildUserInfo(account)

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

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            updateUI(account)
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
            progressDialog.dismiss()
            AlertDialog.Builder(requireActivity())
                .setTitle("Error")
                .setMessage("Failed code: " + e.statusCode)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(true)
                .show()
        }

    }

    private fun signIn() {
        when (authType) {
            TYPE_NONE -> AlertDialog.Builder(requireActivity())
                .setMessage(getString(R.string.select_auth_type))
                .setPositiveButton(R.string.ok) { _, _ -> (requireActivity() as MainActivity).navigateToSettings() }
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
                .show()
            TYPE_GSI -> {
                progressDialog.show()
                googleSignInViewModel.signIn(useIdToken, RC_SIGN_IN)
            }
            else -> throw IllegalStateException("authType is not 1")
        }
    }

    private fun signOut() {
        progressDialog.show()
        googleSignInViewModel.signOut()
    }

    private fun refreshToken() {
        progressDialog.show()
        googleSignInViewModel.refreshToken()
    }

    private fun buildUserInfo(account: GoogleSignInAccount): String {
        var prettyJsonString = ""
        try {
            val json = JSONObject(account.zac())
            if (json.has("tokenId"))
                json.remove("tokenId")
            prettyJsonString = prettyJsonObject(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Log.d(TAG, "buildUserInfo(): $prettyJsonString")
        return prettyJsonString
    }

    private fun setAuthType(type: String) {
        Log.d(TAG, "setAuthType(): type = $type")
        authType = type
    }

    private fun setUseIdToken(useIdToken: Boolean) {
        Log.d(TAG, "setUseIdToken(): useIdToken = $useIdToken")
        this.useIdToken = useIdToken
    }
}
