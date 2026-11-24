package com.toobit.platform.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object UrlRules {
    /** Returns a normalized base URL (trailing slash) or null. Release builds require https. */
    fun normalize(input: String, allowCleartext: Boolean): String? {
        val raw = input.trim()
        val url = raw.toHttpUrlOrNull() ?: return null
        if (!url.isHttps && !allowCleartext) return null
        if (url.host.isEmpty()) return null
        return url.newBuilder().query(null).fragment(null).build().toString().let { if (it.endsWith("/")) it else "$it/" }
    }
}

/**
 * Holds the short-lived access token IN MEMORY ONLY. The server's refresh tokens are single-use and rotate (reuse revokes the
 * whole family), so concurrent 401s must trigger exactly ONE refresh: the mutex plus the "already refreshed?" check do that.
 */
class TokenManager(private val store: SecureStore, private val refreshCall: suspend (String) -> TokenResponse) {
    @Volatile var access: String? = null; private set
    @Volatile var role: String = "Viewer"; private set
    private val mutex = Mutex()
    private val _authed = MutableStateFlow(false)
    val authed: StateFlow<Boolean> = _authed

    fun setSession(t: TokenResponse) { access = t.accessToken; role = t.role; store.refreshToken = t.refreshToken; _authed.value = true }
    fun clear() { access = null; store.clearSession(); _authed.value = false }

    /** [failedAccess] = the bearer token that got the 401 (null on cold start). Returns a usable access token or null. */
    suspend fun refreshAfter401(failedAccess: String?): String? = mutex.withLock {
        val current = access
        if (current != null && current != failedAccess) return current       // another caller already refreshed
        val rt = store.refreshToken ?: run { clear(); return null }
        try {
            val t = refreshCall(rt); setSession(t); t.accessToken
        } catch (e: HttpException) {
            if (e.code() in 400..403) clear()                                  // rejected/revoked: back to login
            null
        } catch (e: IOException) {
            null                                                               // offline: keep the session, retry later
        }
    }
}

class TokenAuthenticator(private val tm: TokenManager) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null                        // one retry only
        val failed = response.request.header("Authorization")?.removePrefix("Bearer ")
        val fresh = runBlocking { tm.refreshAfter401(failed) } ?: return null
        return response.request.newBuilder().header("Authorization", "Bearer $fresh").build()
    }
}

object ApiFactory {
    // Deliberately NO HttpLoggingInterceptor: request/response bodies carry tokens and must never reach logcat.
    fun bareClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()

    fun authedClient(tm: TokenManager): OkHttpClient = bareClient().newBuilder()
        .addInterceptor { chain ->
            val t = tm.access
            chain.proceed(if (t == null) chain.request() else chain.request().newBuilder().header("Authorization", "Bearer $t").build())
        }
        .authenticator(TokenAuthenticator(tm)).build()

    fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl).client(client)
        .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType())).build()
}
