package io.awesdroid.awesauth.db

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.GsonBuilder
import io.awesdroid.awesauthkt.R
import io.awesdroid.awesauthkt.db.AuthDatabase
import io.awesdroid.awesauthkt.db.dao.ConfigDao
import io.awesdroid.awesauthkt.db.entity.ConfigEntity
import io.awesdroid.awesauthkt.model.AppAuthConfig
import io.awesdroid.libkt.android.gson.UriAdapter
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import junit.framework.TestCase.assertNotNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * @auther Awesdroid
 */
@RunWith(AndroidJUnit4::class)
class ConfigDaoTest {
    private lateinit var authDatabase: AuthDatabase
    private lateinit var configDao: ConfigDao
    private var expectedConfigEntity: ConfigEntity? = null

    @Before
    fun init() {
        authDatabase = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().context,
            AuthDatabase::class.java
        ).build()
        configDao = authDatabase.configDao()
    }

    @After
    fun destroy() {
        authDatabase.close()
    }

    @Test
    @Throws(Exception::class)
    fun getConfigWhenEmpty() {
        val latch = CountDownLatch(1)
        CompletableFuture.supplyAsync<Any> { configDao.loadConfig(1) }
            .thenAccept {
                assertNotNull(it)
                latch.countDown()
            }
        latch.await(2, TimeUnit.SECONDS)
    }

    private fun readConfig(): Observable<AppAuthConfig> {
        return Observable.just(R.raw.google_config)
            .map { ApplicationProvider.getApplicationContext<Context>().resources.openRawResource(it) }
            .map { InputStreamReader(it, StandardCharsets.UTF_8) }
            .map { GsonBuilder().registerTypeAdapter(Uri::class.java, UriAdapter()).create()
                .fromJson(it, AppAuthConfig::class.java) }
            .subscribeOn(Schedulers.io())
    }

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
        insertConfigIntoDb().subscribe {
            CompletableFuture.supplyAsync<ConfigEntity> { configDao.loadConfig(1) }
                .thenAccept { configEntity ->
                    Log.d(TAG, "getStateFromDb(): configEntity = $configEntity")
                    assertEquals(expectedConfigEntity, configEntity)
                    latch.countDown()
                }
        }
        latch.await(3, TimeUnit.SECONDS)
    }

    companion object {
        private const val TAG = "ConfigDaoTest"
    }
}
