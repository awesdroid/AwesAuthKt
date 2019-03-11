package io.awesdroid.awesauthkt.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import net.openid.appauth.AuthState

/**
 * @author Awesdroid
 */
@Entity(tableName = "state")
data class StateEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0,
    @ColumnInfo(name = "authState")
    var authState: AuthState
)
