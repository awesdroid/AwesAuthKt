package io.awesdroid.awesauthkt.db.converter

import android.util.Log
import androidx.room.TypeConverter
import io.awesdroid.awesauthkt.model.AppAuthState
import io.awesdroid.libkt.common.utils.TAG
import net.openid.appauth.AuthState
import org.json.JSONException

/**
 * @author Awesdroid
 */
class StateConverter {
    @TypeConverter
    fun toState(str: String): AppAuthState? {
        Log.d(TAG, "toState(): str = $str")
        return try {
            AppAuthState(AuthState.jsonDeserialize(str))
        } catch (e: JSONException) {
            e.printStackTrace()
            null
        }

    }

    @TypeConverter
    fun fromState(state: AppAuthState): String {
        Log.d(TAG, "fromState(): appAuthState = $state")
        return state.authState!!.jsonSerializeString()
    }
}
