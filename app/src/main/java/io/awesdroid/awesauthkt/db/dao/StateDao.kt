package io.awesdroid.awesauthkt.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.awesdroid.awesauthkt.db.entity.StateEntity

/**
 * @author Awesdroid
 */
@Dao
interface StateDao {
    @Query("select * from state where id = :id")
    fun loadAppAuthState(id: Int): StateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAppAuthState(stateEntity: StateEntity)

    @Query("select * from state")
    fun loadAll(): List<StateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(appAuthStateEntities: List<StateEntity>)
}
