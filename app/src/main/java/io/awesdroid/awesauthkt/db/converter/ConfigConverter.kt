package io.awesdroid.awesauthkt.db.converter

import android.util.Log
import androidx.room.TypeConverter
import io.awesdroid.awesauthkt.model.AppAuthConfig
import io.awesdroid.libkt.common.utils.TAG

/**
 * @author Awesdroid
 */
class ConfigConverter {

    @TypeConverter
    fun toConfig(str: String): AppAuthConfig {
        Log.d(TAG, "toConfig(): str = $str")
        return AppAuthConfig.create(str)
    }

    @TypeConverter
    fun fromConfig(config: AppAuthConfig): String {
        Log.d(TAG, "fromConfig(): appAuthConfig = $config")
        return config.toString()
    }
}
