package com.toobit.platform

import com.toobit.platform.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

private class FakeStore : SecureStore {
    override var baseUrl: String? = null
    override var refreshToken: String? = "r0"
    override var biometricRequired = false
    override fun clearSession() { refreshToken = null }
}

class SessionTest {
    @Test fun concurrentRefreshesCollapseToOne() = runBlocking {
        val calls = AtomicInteger()
        val store = FakeStore()
        lateinit var tm: TokenManager
        tm = TokenManager(store) { rt -> calls.incrementAndGet(); Thread.sleep(50); assertEquals("r0", rt); TokenResponse("new", null, "r1", "Admin") }
        tm.setSession(TokenResponse("old", null, "r0", "Admin"))
        val results = (1..8).map { async(Dispatchers.Default) { tm.refreshAfter401("old") } }.awaitAll()
        assertEquals(1, calls.get()); assertTrue(results.all { it == "new" }); assertEquals("r1", store.refreshToken)
    }

    @Test fun rejectedRefreshLogsOut() = runBlocking {
        val store = FakeStore()
        val tm = TokenManager(store) { throw retrofit2.HttpException(retrofit2.Response.error<Any>(401, okhttp3.ResponseBody.create(null, ""))) }
        tm.setSession(TokenResponse("old", null, "r0", "Viewer"))
        assertNull(tm.refreshAfter401("old")); assertFalse(tm.authed.value); assertNull(store.refreshToken)
    }

    @Test fun endToEndParallel401sRefreshOnce() {
        val refreshHits = AtomicInteger()
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path == "/api/auth/refresh" -> { refreshHits.incrementAndGet(); Thread.sleep(50)
                    MockResponse().setBody("""{"accessToken":"new","refreshToken":"r1","role":"Admin","accessExpires":"x"}""") }
                request.getHeader("Authorization") == "Bearer new" -> MockResponse().setBody("""{"on":true}""")
                else -> MockResponse().setResponseCode(401)
            }
        }
        server.start()
        try {
            val base = server.url("/").toString()
            val auth = ApiFactory.retrofit(base, ApiFactory.bareClient()).create(AuthApi::class.java)
            val tm = TokenManager(FakeStore()) { auth.refresh(RefreshRequest(it)) }
            tm.setSession(TokenResponse("old", null, "r0", "Admin"))
            val api = ApiFactory.retrofit(base, ApiFactory.authedClient(tm)).create(PlatformApi::class.java)
            val out = runBlocking { (1..4).map { async(Dispatchers.IO) { api.killSwitch() } }.awaitAll() }
            assertTrue(out.all { it.on }); assertEquals(1, refreshHits.get())
        } finally { server.shutdown() }
    }
}
