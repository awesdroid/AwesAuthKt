package io.awesdroid.awesauthkt.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.GsonBuilder
import io.awesdroid.awesauthkt.data.R
import io.awesdroid.awesauthkt.data.db.AuthDatabase
import io.awesdroid.awesauthkt.data.db.entity.ConfigEntity
import io.awesdroid.awesauthkt.data.db.entity.StateEntity
import io.awesdroid.awesauthkt.data.exception.RecoverableException
import io.awesdroid.awesauthkt.data.model.AppAuthConfig
import io.awesdroid.libkt.android.gson.UriAdapter
import io.awesdroid.libkt.common.executors.Dispatchers.CACHED
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.openid.appauth.AuthState
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets
import kotlin.coroutines.CoroutineContext

/**
 * @author Awesdroid
 */
class AppAuthRepository(context: Context): CoroutineScope, AuthRepository<AppAuthConfig, AuthState> {
    override val coroutineContext: CoroutineContext
        get() = CACHED

    private val recoverableErrorHandler = CoroutineExceptionHandler { _, ex ->
        throw RecoverableException.DatabaseError(ex)
    }

    private var contextRef = WeakReference<Context>(null)

    init {
        contextRef = WeakReference(context)
    }

    override fun loadAuthConfiguration(): AppAuthConfig {
        return Single.fromCallable {
            AuthDatabase.instance!!.configDao().loadConfig(1)
                ?.appAuthConfig
                ?:let {
                    val config = readAppAuthConfig().toFuture().get()
                    storeAuthConfiguration(config)
                    config
                }
        }
            .subscribeOn(Schedulers.io())
            .toFuture()
            .get()
    }

    override fun storeAuthConfiguration(configuration: AppAuthConfig) {
        launch(recoverableErrorHandler) {
            AuthDatabase.instance!!.configDao().insertConfig(ConfigEntity(1, configuration))
        }
    }

    override fun storeAuthState(state: AuthState) {
        launch(recoverableErrorHandler) {
            AuthDatabase.instance!!.stateDao().insertAppAuthState(StateEntity(1, state))
        }
    }

    override fun clean() {
        AuthDatabase.instance!!.destroy()
    }

    override fun loadAuthState(): AuthState? {
        return AuthDatabase.instance!!.stateDao().loadAppAuthState(1)?.authState
    }

    private fun readAppAuthConfig(): Single<AppAuthConfig> {
        return Single.just(R.raw.google_config)
            .map { contextRef.get()!!.resources.openRawResource(it) }
            .map { InputStreamReader(it, StandardCharsets.UTF_8) }
            .map { GsonBuilder().registerTypeAdapter(Uri::class.java, UriAdapter()).create().fromJson(it, AppAuthConfig::class.java) }
    }
}
