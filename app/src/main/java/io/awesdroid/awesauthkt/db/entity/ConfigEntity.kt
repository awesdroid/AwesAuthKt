package io.awesdroid.awesauthkt.db.entity

import android.net.Uri
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.GsonBuilder
import io.awesdroid.awesauthkt.model.AppAuthConfig
import io.awesdroid.libkt.android.gson.UriAdapter

/**
 * @author Awesdroid
 */
@Entity(tableName = "config")
class ConfigEntity {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    lateinit var appAuthConfig: AppAuthConfig

    constructor(){}

    @Ignore
    constructor(id: Int, appAuthConfig: AppAuthConfig) {
        this.id = id
        this.appAuthConfig = appAuthConfig
    }

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
