package com.toobit.platform.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Tolerant parsing: the server may add fields; missing optional fields never crash the UI. */
val AppJson = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false; isLenient = true }

@Serializable data class LoginRequest(val email: String, val password: String)
@Serializable data class RefreshRequest(val refreshToken: String)
@Serializable data class TokenResponse(val accessToken: String, val accessExpires: String? = null, val refreshToken: String, val role: String = "Viewer")

@Serializable data class TrendingSymbol(
    val rank: Int, val symbol: String, val score: Double,
    val quoteVolume: Double = 0.0, val momentum: Double = 0.0, val volumeSpikeRatio: Double = 0.0,
    val normVolume: Double = 0.0, val normMomentum: Double = 0.0, val normSpike: Double = 0.0,
    val weightsJson: String? = null, val explanation: String? = null,
)
@Serializable data class ScannerTop(val stale: Boolean = false, val lastSuccess: String? = null, val items: List<TrendingSymbol> = emptyList())
@Serializable data class OverrideRequest(val symbol: String, val pinned: Boolean)

@Serializable data class PriceTick(val symbol: String, val last: Double, val open: Double = 0.0, val high: Double = 0.0, val low: Double = 0.0, val quoteVolume: Double = 0.0, val at: String? = null)
@Serializable data class Prices(
    val wsConnected: Boolean = false, val marketDataStale: Boolean = false, val scannerStale: Boolean = false,
    val lastTickAt: String? = null, val items: List<PriceTick> = emptyList(),
)

@Serializable data class Position(val id: String, val symbol: String, val side: String = "", val quantity: Double = 0.0, val entryPrice: Double = 0.0, val reconciledAt: String? = null)
@Serializable data class CloseRequest(val confirm: Boolean, val referencePrice: Double)
@Serializable data class CloseResponse(val clientOrderId: String? = null, val state: String? = null)

@Serializable data class Order(
    val id: String, val clientOrderId: String, val toobitOrderId: String? = null, val symbol: String,
    val side: Int = 0, val quantity: Double = 0.0, val limitPrice: Double? = null,
    val stopLoss: Double? = null, val takeProfit: Double? = null,
    val stateMachineState: Int, val unknownRetries: Int = 0, val createdAt: String? = null,
)
@Serializable data class OrderEvent(val id: String = "", val from: Int = 0, val to: Int = 0, val reason: String? = null, val occurredAt: String? = null)

@Serializable data class Signal(
    val id: String, val symbol: String, val action: String, val confidence: Double = 0.0,
    val modelVersion: String? = null, val featuresJson: String? = null, val explainJson: String? = null,
    val shadow: Boolean = false, val createdAt: String? = null,
)
@Serializable data class RiskEvent(val id: String = "", val symbol: String? = null, val rule: String = "", val reason: String? = null, val createdAt: String? = null)
@Serializable data class ReconciliationEvent(val id: String = "", val symbol: String? = null, val kind: String = "", val detail: String? = null, val pausedSymbol: Boolean = false, val createdAt: String? = null)

@Serializable data class KillSwitch(val on: Boolean)
@Serializable data class KillSwitchRequest(val on: Boolean, val reason: String, val confirm: String)
@Serializable data class Flags(val automatedTrading: Boolean = false, val scannerWeightSet: String = "")
@Serializable data class Settings(val flags: Flags = Flags(), val editableAtRuntime: List<String> = emptyList())
