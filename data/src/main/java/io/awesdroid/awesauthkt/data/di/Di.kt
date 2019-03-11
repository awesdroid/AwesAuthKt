package io.awesdroid.awesauthkt.data.di

import org.kodein.di.Kodein

/**
 * @author Awesdroid
 */
const val DI_CONTEXT_TAG = "di_context"
const val DI_ACTIVITY_TAG = "di_activity"

object DI {
    var kodein: Kodein? = null
    fun init(parent: Kodein) {
        kodein = parent
        kodein = Kodein {
            extend(parent)
            importOnce(httpModule)
        }
    }

    fun clear() {
        kodein = null
    }
}