package io.awesdroid.awesauth.db

import android.net.Uri
import android.util.Log
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.GsonBuilder
import io.awesdroid.awesauthkt.db.AuthDatabase
import io.awesdroid.awesauthkt.db.dao.StateDao
import io.awesdroid.awesauthkt.db.entity.StateEntity
import io.awesdroid.awesauthkt.model.AppAuthState
import io.awesdroid.libkt.android.gson.UriAdapter
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import junit.framework.TestCase.assertNotNull
import net.openid.appauth.AuthState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * @auther Awesdroid
 */
@RunWith(AndroidJUnit4::class)
class StateDaoTest {
    private lateinit var authDatabase: AuthDatabase
    private lateinit var stateDao: StateDao
    private var expectedState: AuthState? = null

    @Before
    fun init() {
        authDatabase = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().context,
            AuthDatabase::class.java
        )
            .build()
        stateDao = authDatabase.stateDao()
    }

    @After
    fun destroy() {
        authDatabase.close()
    }

    @Test
    @Throws(Exception::class)
    fun getConfigWhenEmpty() {
        val latch = CountDownLatch(1)
        CompletableFuture.supplyAsync<Any> { stateDao.loadAppAuthState(1) }
            .thenAccept {
                assertNotNull(it)
                latch.countDown()
            }

        latch.await(2, TimeUnit.SECONDS)
    }

    private fun readState(): Observable<AuthState> {
        return Observable.defer<InputStream> {
            val inputStream =
                javaClass.getResourceAsStream("state.json") ?: return@defer Observable.empty<InputStream>()
            Observable.just(inputStream)
            }
            .map { InputStreamReader(it, StandardCharsets.UTF_8) }
            .map { GsonBuilder().registerTypeAdapter(Uri::class.java, UriAdapter()).create()
                    .fromJson(it, AuthState::class.java) }
            .subscribeOn(Schedulers.io())
    }

    private fun insertConfigIntoDb(): Observable<Boolean> {
        return readState().map {
            expectedState = it
            Log.d(TAG, "insertConfigIntoDb(): expectedState = " + expectedState!!.jsonSerializeString())
            stateDao.insertAppAuthState(StateEntity(1, AppAuthState(it)))
            true
        }
    }

    @Test
    @Throws(Exception::class)
    fun getStateFromDb() {
        val latch = CountDownLatch(1)
        insertConfigIntoDb().subscribe {
            CompletableFuture.supplyAsync<StateEntity> { stateDao.loadAppAuthState(1) }
                .thenAccept {stateEntity ->
                    assertEquals(1, stateEntity.id)
                    assertEquals(
                        expectedState!!.jsonSerializeString(),
                        stateEntity.appAuthState?.authState?.jsonSerializeString()
                    )
                    latch.countDown()
                }
                .exceptionally { e ->
                    fail(e.message)
                    null
                }
        }
        latch.await(3, TimeUnit.SECONDS)
    }

    companion object {
        private const val TAG = "StateDaoTest"
    }
}
