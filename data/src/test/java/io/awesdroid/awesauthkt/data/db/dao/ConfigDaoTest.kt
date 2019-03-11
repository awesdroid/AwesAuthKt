package io.awesdroid.awesauthkt.data.db.dao

import android.net.Uri
import android.util.Log
import androidx.room.Room
import com.google.gson.GsonBuilder
import io.awesdroid.awesauthkt.data.AppTestCase
import io.awesdroid.awesauthkt.data.R
import io.awesdroid.awesauthkt.data.db.AuthDatabase
import io.awesdroid.awesauthkt.data.db.entity.ConfigEntity
import io.awesdroid.awesauthkt.data.model.AppAuthConfig
import io.awesdroid.libkt.android.gson.UriAdapter
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * @author Awesdroid
 */
class ConfigDaoTest: AppTestCase() {
    private lateinit var authDatabase: AuthDatabase
    private lateinit var configDao: ConfigDao
    private var expectedConfigEntity: ConfigEntity? = null

    @Before
    fun init() {
        authDatabase = Room.inMemoryDatabaseBuilder(
            context(),
            AuthDatabase::class.java
        ).build()
        configDao = authDatabase.configDao()
    }

    @After
    fun destroy() {
        authDatabase.close()
    }

    @Test
    fun `get Null config when db is empty`() {
        runBlocking(Dispatchers.Default) {
            val config = configDao.loadConfig(1)
            assertNull(config)
        }
    }

    private fun readConfig()=
        Observable.just(R.raw.google_config)
            .map { context().resources.openRawResource(it) }
            .map { InputStreamReader(it, StandardCharsets.UTF_8) }
            .map { GsonBuilder().registerTypeAdapter(Uri::class.java, UriAdapter()).create()
                .fromJson(it, AppAuthConfig::class.java) }
            .subscribeOn(Schedulers.io())

    private fun insertConfigIntoDb(): Observable<Boolean> {
        return readConfig().map {
            expectedConfigEntity = ConfigEntity(1, it)
            configDao.insertConfig(expectedConfigEntity!!)
            true
        }
    }

    @Test
    @Throws(Exception::class)
    fun getConfigFromDb() {
        val latch = CountDownLatch(1)
        insertConfigIntoDb().subscribe ({
            CompletableFuture.supplyAsync<ConfigEntity> { configDao.loadConfig(1) }
                .thenAccept { configEntity ->
                    Log.d(TAG, "getStateFromDb(): configEntity = $configEntity")
                    assertEquals(expectedConfigEntity, configEntity)
                    latch.countDown()
                }
        },
            {
                Log.w(TAG, "getConfigFromDb(): e = $it")
            })
        latch.await(3, TimeUnit.SECONDS)
    }

    companion object {
        private const val TAG = "ConfigDaoTest"
    }
}
