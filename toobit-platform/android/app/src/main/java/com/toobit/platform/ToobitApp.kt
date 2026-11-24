package com.toobit.platform

import android.app.Application
import com.toobit.platform.data.*
import kotlinx.coroutines.flow.StateFlow

class AppContainer(val store: SecureStore) {
    @Volatile private var authApi: AuthApi? = null
    @Volatile var api: PlatformApi? = null; private set
    val tokens = TokenManager(store) { rt -> authApi!!.refresh(RefreshRequest(rt)) }
    val authed: StateFlow<Boolean> get() = tokens.authed

    private fun build(baseUrl: String) {
        authApi = ApiFactory.retrofit(baseUrl, ApiFactory.bareClient()).create(AuthApi::class.java)
        api = ApiFactory.retrofit(baseUrl, ApiFactory.authedClient(tokens)).create(PlatformApi::class.java)
    }

    /** Returns null on success, otherwise a short error key: "url" | "credentials" | "network". */
    suspend fun login(urlInput: String, email: String, password: String): String? {
        val base = UrlRules.normalize(urlInput, BuildConfig.DEBUG) ?: return "url"
        build(base)
        return try {
            tokens.setSession(authApi!!.login(LoginRequest(email.trim(), password)))
            store.baseUrl = base; null
        } catch (e: retrofit2.HttpException) { "credentials" } catch (e: java.io.IOException) { "network" }
    }

    /** Cold start: try to resume with the stored refresh token. */
    suspend fun restore() {
        val base = store.baseUrl ?: return
        if (store.refreshToken == null) return
        build(base)
        tokens.refreshAfter401(null)
    }

    suspend fun logout() {
        val rt = store.refreshToken
        if (rt != null) runCatching { authApi?.logout(RefreshRequest(rt)) }     // best effort; local clear always happens
        tokens.clear()
    }
}

class ToobitApp : Application() {
    lateinit var container: AppContainer
    override fun onCreate() { super.onCreate(); container = AppContainer(EncryptedStore(this)) }
}
