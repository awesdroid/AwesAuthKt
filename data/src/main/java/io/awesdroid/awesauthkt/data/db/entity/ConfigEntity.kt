package io.awesdroid.awesauthkt.data.db.entity

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.GsonBuilder
import io.awesdroid.awesauthkt.data.model.AppAuthConfig
import io.awesdroid.libkt.android.gson.UriAdapter

/**
 * @author Awesdroid
 */
@Entity(tableName = "config")
data class ConfigEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0,
    @ColumnInfo(name = "config")
    var appAuthConfig: AppAuthConfig
) {
    override fun equals(obj: Any?): Boolean {
        return if (obj !is ConfigEntity) {
            false
        } else obj.toString() == this.toString()
    }

    override fun toString(): String {
        return GsonBuilder()
            .registerTypeAdapter(Uri::class.java, UriAdapter())
            .create()
            .toJson(this)
    }
}
