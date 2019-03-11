package io.awesdroid.awesauthkt.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog




/**
 * @author Awesdroid
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = AppStub::class, sdk = [28], manifest = Config.NONE, shadows = [ShadowLog::class])
abstract class AppTestCase {

    @get:Rule
    var injectMocksRule = TestRule { base, _ ->
        MockitoAnnotations.initMocks(this@AppTestCase)
        base
    }

    @Before
    fun setUp() {
        ShadowLog.stream = System.out
    }

    companion object {
        fun context(): Context {
            return ApplicationProvider.getApplicationContext<Context>()
        }
    }
}
