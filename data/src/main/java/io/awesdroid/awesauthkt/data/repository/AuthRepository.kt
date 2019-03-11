package io.awesdroid.awesauthkt.data.repository

/**
 * @author Awesdroid
 */
interface AuthRepository<Configuration, State> {
    fun loadAuthConfiguration(): Configuration
    fun loadAuthState(): State?
    fun storeAuthConfiguration(configuration: Configuration)
    fun storeAuthState(state: State)
    fun clean()
}