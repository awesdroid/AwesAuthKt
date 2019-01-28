package io.awesdroid.awesauthkt.db.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import io.awesdroid.awesauthkt.model.AppAuthState

/**
 * @auther Awesdroid
 */
@Entity(tableName = "state")
class StateEntity {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    var appAuthState: AppAuthState? = null

    constructor(){}

    @Ignore
    constructor(id: Int, appAuthState: AppAuthState) {
        this.id = id
        this.appAuthState = appAuthState
    }
}
