package io.awesdroid.awesauthkt.data.db.dao

import android.net.Uri
import androidx.room.Room
import com.google.gson.GsonBuilder
import io.awesdroid.awesauthkt.data.AppTestCase
import io.awesdroid.awesauthkt.data.db.AuthDatabase
import io.awesdroid.awesauthkt.data.db.entity.StateEntity
import io.awesdroid.libkt.android.gson.UriAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthState
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * @author Awesdroid
 */
class StateDaoTest: AppTestCase() {
    private lateinit var authDatabase: AuthDatabase
    private lateinit var stateDao: StateDao
    private var expectedState: AuthState? = null

    @Before
    fun init() {
        authDatabase = Room.inMemoryDatabaseBuilder(context(), AuthDatabase::class.java).build()
        stateDao = authDatabase.stateDao()
    }

    @After
    fun destroy() {
        authDatabase.close()
    }

    @Test
    fun `get Null config when db is empty`() {
        runBlocking(Dispatchers.Default) {
            val config = stateDao.loadAppAuthState(1)
            Assert.assertNull(config)
        }
    }


    @Test
    fun `read same state from db after insert one`() {
        runBlocking(Dispatchers.Default) {
            insertConfigIntoDb()
            val state= stateDao.loadAppAuthState(1)
            assertEquals(1, state?.id)
            assertEquals(expectedState!!.jsonSerializeString(), state?.authState?.jsonSerializeString())
        }
    }

    private fun readState(): AuthState {
        return InputStreamReader(javaClass.getResourceAsStream("state.json"), StandardCharsets.UTF_8)
            .let {
                GsonBuilder()
                    .registerTypeAdapter(Uri::class.java, UriAdapter())
                    .create()
                    .fromJson(it, AuthState::class.java)
            }
    }

    private fun insertConfigIntoDb(): Boolean {
        return readState().let {
            expectedState = it
            stateDao.insertAppAuthState(StateEntity(1, it))
            true
        }
    }
}
