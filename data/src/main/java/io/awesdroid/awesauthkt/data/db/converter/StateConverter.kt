package io.awesdroid.awesauthkt.data.db.converter

import android.util.Log
import androidx.room.TypeConverter
import io.awesdroid.libkt.common.utils.TAG
import net.openid.appauth.AuthState
import org.json.JSONException

/**
 * @author Awesdroid
 */
class StateConverter {
    @TypeConverter
    fun toState(str: String): AuthState? {
        Log.d(TAG, "toState(): str = $str")
        return try {
            AuthState.jsonDeserialize(str)
        } catch (e: JSONException) {
            e.printStackTrace()
            null
        }

    }

    @TypeConverter
    fun fromState(state: AuthState): String {
        Log.d(TAG, "fromState(): state = $state")
        return state.jsonSerializeString()
    }
}
