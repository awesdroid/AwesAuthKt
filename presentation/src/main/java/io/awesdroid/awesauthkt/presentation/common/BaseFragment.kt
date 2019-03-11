package io.awesdroid.awesauthkt.presentation.common

import androidx.fragment.app.Fragment
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein

/**
 * @author Awesdroid
 */
abstract class BaseFragment: Fragment(), KodeinAware {
    private val parentKodein by closestKodein()

    override val kodein = Kodein.lazy {
        extend(parentKodein)
    }
}