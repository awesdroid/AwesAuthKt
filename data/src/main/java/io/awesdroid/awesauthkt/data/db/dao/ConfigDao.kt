package io.awesdroid.awesauthkt.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.awesdroid.awesauthkt.data.db.entity.ConfigEntity

/**
 * @author Awesdroid
 */
@Dao
interface ConfigDao {
    @Query("select * from config where id = :id")
    fun loadConfig(id: Int): ConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertConfig(config: ConfigEntity)
}
