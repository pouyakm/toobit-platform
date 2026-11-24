package com.toobit.platform.data

import retrofit2.http.*

/** Unauthenticated endpoints (no Authorization header, no Authenticator). */
interface AuthApi {
    @POST("api/auth/login") suspend fun login(@Body r: LoginRequest): TokenResponse
    @POST("api/auth/refresh") suspend fun refresh(@Body r: RefreshRequest): TokenResponse
    @POST("api/auth/logout") suspend fun logout(@Body r: RefreshRequest)
}

/** Everything the dashboard uses. No Toobit/exchange endpoints: the phone only ever talks to our own .NET API. */
interface PlatformApi {
    @GET("api/scanner/top") suspend fun scannerTop(@Query("n") n: Int = 20): ScannerTop
    @POST("api/scanner/overrides") suspend fun override(@Body r: OverrideRequest)
    @GET("api/prices") suspend fun prices(): Prices
    @GET("api/positions") suspend fun positions(): List<Position>
    @POST("api/positions/{symbol}/close") suspend fun close(@Path("symbol") symbol: String, @Body r: CloseRequest): CloseResponse
    @GET("api/orders") suspend fun orders(): List<Order>
    @GET("api/orders/{id}/events") suspend fun orderEvents(@Path("id") id: String): List<OrderEvent>
    @GET("api/signals") suspend fun signals(): List<Signal>
    @GET("api/risk-events") suspend fun riskEvents(): List<RiskEvent>
    @GET("api/reconciliation-events") suspend fun reconEvents(): List<ReconciliationEvent>
    @GET("api/killswitch") suspend fun killSwitch(): KillSwitch
    @POST("api/killswitch") suspend fun setKillSwitch(@Body r: KillSwitchRequest): KillSwitch
    @GET("api/settings") suspend fun settings(): Settings
    @PUT("api/settings/flags") suspend fun putFlags(@Body f: Flags)
    @GET("api/pauses") suspend fun pauses(): List<String>
    @DELETE("api/pauses/{symbol}") suspend fun clearPause(@Path("symbol") symbol: String)
}
