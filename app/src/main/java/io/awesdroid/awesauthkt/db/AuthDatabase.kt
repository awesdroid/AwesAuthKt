package io.awesdroid.awesauthkt.db


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.awesdroid.awesauthkt.db.converter.ConfigConverter
import io.awesdroid.awesauthkt.db.converter.StateConverter
import io.awesdroid.awesauthkt.db.dao.ConfigDao
import io.awesdroid.awesauthkt.db.dao.StateDao
import io.awesdroid.awesauthkt.db.entity.ConfigEntity
import io.awesdroid.awesauthkt.db.entity.StateEntity
import io.awesdroid.awesauthkt.utils.ActivityHelper
import io.awesdroid.awesauthkt.utils.mutableLazy

/**
 * @author Awesdroid
 */
@Database(entities = [StateEntity::class, ConfigEntity::class], version = 1)
@TypeConverters(StateConverter::class, ConfigConverter::class)
abstract class AuthDatabase : RoomDatabase() {

    abstract fun stateDao(): StateDao
    abstract fun configDao(): ConfigDao

    fun destroy() {
        instance.takeIf { isOpen }.run { close() }
        instance = null
    }

    companion object {
        private const val DB_NAME = "appauth.db"
        var instance: AuthDatabase? by mutableLazy { buildDatabase(ActivityHelper.getContext()) }

        private fun buildDatabase(context: Context): AuthDatabase {
            return Room.databaseBuilder(context, AuthDatabase::class.java, DB_NAME).build()
        }
    }
}
