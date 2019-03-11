package io.awesdroid.awesauthkt.data.db


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.awesdroid.awesauthkt.data.db.converter.ConfigConverter
import io.awesdroid.awesauthkt.data.db.converter.StateConverter
import io.awesdroid.awesauthkt.data.db.dao.ConfigDao
import io.awesdroid.awesauthkt.data.db.dao.StateDao
import io.awesdroid.awesauthkt.data.db.entity.ConfigEntity
import io.awesdroid.awesauthkt.data.db.entity.StateEntity
import io.awesdroid.awesauthkt.data.di.DI
import io.awesdroid.awesauthkt.data.di.DI_CONTEXT_TAG
import io.awesdroid.libkt.common.utils.mutableLazy
import org.kodein.di.generic.instance

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
        var instance: AuthDatabase? by mutableLazy {
            val context: Context by DI.kodein!!.instance(DI_CONTEXT_TAG)
            buildDatabase(context)
        }

        private fun buildDatabase(context: Context): AuthDatabase {
            return Room.databaseBuilder(context, AuthDatabase::class.java, DB_NAME).build()
        }
    }
}
