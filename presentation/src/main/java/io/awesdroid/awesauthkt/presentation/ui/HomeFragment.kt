package io.awesdroid.awesauthkt.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import io.awesdroid.awesauthkt.presentation.R
import io.awesdroid.awesauthkt.presentation.common.BaseFragment
import kotlinx.android.synthetic.main.fragment_home.view.*

/**
 * @author Awesdroid
 */
class HomeFragment: BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }

        rootView.button_appauth.setOnClickListener {
            findNavController().navigate(R.id.action_dest_home_to_dest_appauth, null, options)
        }
        rootView.button_gsi.setOnClickListener {
            findNavController().navigate(R.id.action_dest_home_to_dest_gsi, null, options)
        }

        return rootView
    }
}