package com.toobit.platform.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.toobit.platform.AppContainer
import com.toobit.platform.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface Ui<out T> {
    data object Loading : Ui<Nothing>
    /** [failed] = the latest refresh failed but last-known data is still shown (stale). */
    data class Ok<T>(val v: T, val failed: Boolean = false) : Ui<T>
    data class Fail(val msg: String) : Ui<Nothing>
}

data class RiskBundle(val risk: List<RiskEvent>, val recon: List<ReconciliationEvent>, val paused: List<String>)
data class SettingsBundle(val settings: Settings, val kill: KillSwitch)

fun <T> poll(intervalMs: Long, block: suspend () -> T): Flow<Ui<T>> = flow {
    var last: T? = null; var has = false
    while (true) {
        try { val v = block(); last = v; has = true; emit(Ui.Ok(v)) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            @Suppress("UNCHECKED_CAST")
            emit(if (has) Ui.Ok(last as T, failed = true) else Ui.Fail(e::class.java.simpleName))
        }
        delay(intervalMs)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(private val c: AppContainer) : ViewModel() {
    private val api: PlatformApi get() = c.api!!
    private val tick = MutableStateFlow(0)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages
    val pinned = MutableStateFlow<Set<String>>(emptySet())      // local view of pins (server list is in-memory and not exposed)

    private fun <T> polled(ms: Long, block: suspend () -> T): StateFlow<Ui<T>> =
        tick.flatMapLatest { poll(ms, block) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Ui.Loading)

    val scanner = polled(10_000) { api.scannerTop(20) }
    val prices = polled(5_000) { api.prices() }
    val positions = polled(10_000) { api.positions() }
    val orders = polled(10_000) { api.orders() }
    val signals = polled(30_000) { api.signals() }
    val risk = polled(30_000) { RiskBundle(api.riskEvents(), api.reconEvents(), api.pauses()) }
    val settings = polled(15_000) { SettingsBundle(api.settings(), api.killSwitch()) }
    val kill = polled(10_000) { api.killSwitch() }

    val role: String get() = c.tokens.role
    val canTrade: Boolean get() = role == "Admin" || role == "Trader"
    val isAdmin: Boolean get() = role == "Admin"
    val serverUrl: String get() = c.store.baseUrl.orEmpty()

    private fun act(okMsg: String, errMsg: String, block: suspend () -> Unit) = viewModelScope.launch {
        try { block(); _messages.emit(okMsg); tick.value++ }
        catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) { _messages.emit("$errMsg (HTTP ${e.code()})") }
        catch (e: Exception) { _messages.emit(errMsg) }
    }

    fun togglePin(symbol: String) {
        val now = symbol !in pinned.value
        act("OK", "Error") { api.override(OverrideRequest(symbol, now)); pinned.value = if (now) pinned.value + symbol else pinned.value - symbol }
    }
    fun closePosition(p: Position, refPrice: Double) = act("OK", "Error") { api.close(p.symbol, CloseRequest(confirm = true, referencePrice = refPrice)) }
    fun clearPause(symbol: String) = act("OK", "Error") { api.clearPause(symbol) }
    fun setKill(on: Boolean, reason: String, confirm: String) = act("OK", "Error") { api.setKillSwitch(KillSwitchRequest(on, reason.trim(), confirm.trim())) }
    fun saveFlags(f: Flags) = act("OK", "Error") { api.putFlags(f) }
    suspend fun orderEvents(id: String): List<OrderEvent> = api.orderEvents(id)
    fun logout() { viewModelScope.launch { c.logout() } }
    fun setBiometric(v: Boolean) { c.store.biometricRequired = v }
    val biometricRequired: Boolean get() = c.store.biometricRequired

    class Factory(private val c: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(c) as T
    }
}
