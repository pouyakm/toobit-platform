package com.toobit.platform.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.toobit.platform.R
import com.toobit.platform.data.*
import com.toobit.platform.domain.*

private val pad = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)

@Composable
fun ScannerScreen(vm: AppViewModel) {
    val s by vm.scanner.collectAsStateWithLifecycleCompat()
    val prices by vm.prices.collectAsStateWithLifecycleCompat()
    val pinned by vm.pinned.collectAsStateWithLifecycleCompat()
    val live = (prices as? Ui.Ok)?.v?.items?.associateBy { it.symbol }.orEmpty()
    UiBox(s, isEmpty = { it.items.isEmpty() }) { top ->
        LazyColumn(Modifier.fillMaxSize()) {
            items(top.items, key = { it.symbol }) { t ->
                var open by rememberSaveable(t.symbol) { mutableStateOf(false) }
                Card(pad.fillMaxWidth().clickable { open = !open }) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("#${t.rank}  ${t.symbol}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(fmt(t.score, 3), style = MaterialTheme.typography.titleMedium)
                        }
                        live[t.symbol]?.let { Text(stringResource(R.string.scanner_price) + ": " + fmt(it.last)) }
                        Text("${stringResource(R.string.scanner_volume)} ${fmt(t.normVolume, 2)} · ${stringResource(R.string.scanner_momentum)} ${fmt(t.normMomentum, 2)} · ${stringResource(R.string.scanner_spike)} ${fmt(t.normSpike, 2)}",
                            style = MaterialTheme.typography.bodySmall)
                        if (open) {
                            Text(stringResource(R.string.scanner_why), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                            Text(t.explanation.orEmpty(), style = MaterialTheme.typography.bodySmall)
                        }
                        if (vm.canTrade) TextButton(onClick = { vm.togglePin(t.symbol) }) {
                            Text(stringResource(if (t.symbol in pinned) R.string.scanner_unpin else R.string.scanner_pin))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PositionsScreen(vm: AppViewModel) {
    val s by vm.positions.collectAsStateWithLifecycleCompat()
    val prices by vm.prices.collectAsStateWithLifecycleCompat()
    var closing by remember { mutableStateOf<Position?>(null) }
    UiBox(s, isEmpty = { it.isEmpty() }) { list ->
        LazyColumn(Modifier.fillMaxSize()) {
            items(list, key = { it.id }) { p ->
                Card(pad.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${p.symbol} · ${p.side}", style = MaterialTheme.typography.titleMedium)
                        Text("qty ${fmt(p.quantity)} @ ${fmt(p.entryPrice)}")
                        if (vm.canTrade) TextButton(onClick = { closing = p }) { Text(stringResource(R.string.positions_close)) }
                    }
                }
            }
        }
    }
    closing?.let { p ->
        val ref = (prices as? Ui.Ok)?.v?.items?.firstOrNull { it.symbol == p.symbol }?.last ?: p.entryPrice
        ConfirmDialog(stringResource(R.string.positions_close), stringResource(R.string.positions_close_confirm, p.symbol),
            onConfirm = { vm.closePosition(p, ref); closing = null }, onDismiss = { closing = null })
    }
}

@Composable
fun OrdersScreen(vm: AppViewModel) {
    val s by vm.orders.collectAsStateWithLifecycleCompat()
    UiBox(s, isEmpty = { it.isEmpty() }) { list ->
        LazyColumn(Modifier.fillMaxSize()) {
            items(list, key = { it.id }) { o ->
                var open by rememberSaveable(o.id) { mutableStateOf(false) }
                Card(pad.fillMaxWidth().clickable { open = !open }) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${o.symbol} · ${if (o.side == 0) "BUY" else "SELL"} · ${OrderStates.name(o.stateMachineState)}", style = MaterialTheme.typography.titleMedium)
                        Text("qty ${fmt(o.quantity)}" + (o.limitPrice?.let { " @ ${fmt(it)}" } ?: ""), style = MaterialTheme.typography.bodySmall)
                        if (OrderStates.isUnknown(o.stateMachineState))
                            Text(stringResource(R.string.orders_retries, o.unknownRetries), color = MaterialTheme.colorScheme.error)
                        if (open) OrderTrail(vm, o.id)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderTrail(vm: AppViewModel, id: String) {
    var events by remember(id) { mutableStateOf<List<OrderEvent>?>(null) }
    var failed by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id) { try { events = vm.orderEvents(id) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { failed = true } }
    Text(stringResource(R.string.orders_trail), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    when {
        failed -> Text(stringResource(R.string.error), color = MaterialTheme.colorScheme.error)
        events == null -> LinearProgressIndicator()
        else -> events!!.forEach { Text("${OrderStates.name(it.from)} → ${OrderStates.name(it.to)}  ${it.reason.orEmpty()}", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun SignalsScreen(vm: AppViewModel) {
    val s by vm.signals.collectAsStateWithLifecycleCompat()
    UiBox(s, isEmpty = { it.isEmpty() }) { list ->
        LazyColumn(Modifier.fillMaxSize()) {
            item { Text(stringResource(R.string.signals_note), style = MaterialTheme.typography.bodySmall, modifier = pad) }
            items(list, key = { it.id }) { g ->
                Card(pad.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${g.symbol} · ${g.action} · ${fmt(g.confidence, 2)}" + if (g.shadow) " · ${stringResource(R.string.signals_shadow)}" else "", style = MaterialTheme.typography.titleMedium)
                        g.modelVersion?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        val top = Explain.topFeatures(g.explainJson ?: g.featuresJson)
                        if (top.isNotEmpty()) {
                            Text(stringResource(R.string.signals_features), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
                            top.forEach { (k, v) -> Text("$k: ${fmt(v)}", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RiskScreen(vm: AppViewModel) {
    val s by vm.risk.collectAsStateWithLifecycleCompat()
    UiBox(s) { b ->
        LazyColumn(Modifier.fillMaxSize()) {
            item { Text(stringResource(R.string.risk_paused), style = MaterialTheme.typography.titleMedium, modifier = pad) }
            if (b.paused.isEmpty()) item { Text(stringResource(R.string.none), modifier = pad) }
            items(b.paused, key = { "p$it" }) { sym ->
                Row(pad.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(sym, Modifier.weight(1f))
                    if (vm.isAdmin) TextButton(onClick = { vm.clearPause(sym) }) { Text(stringResource(R.string.risk_clear_pause)) }
                }
            }
            item { Text(stringResource(R.string.risk_title), style = MaterialTheme.typography.titleMedium, modifier = pad) }
            items(b.risk, key = { "r${it.id}" }) { Text("${it.symbol.orEmpty()} ${it.rule}: ${it.reason.orEmpty()}", modifier = pad, style = MaterialTheme.typography.bodySmall) }
            item { Text(stringResource(R.string.risk_recon), style = MaterialTheme.typography.titleMedium, modifier = pad) }
            items(b.recon, key = { "c${it.id}" }) { Text("${it.symbol.orEmpty()} ${it.kind}: ${it.detail.orEmpty()}" + if (it.pausedSymbol) " ⏸" else "", modifier = pad, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, biometricAvailable: Boolean, onLogout: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycleCompat()
    UiBox(s) { b ->
        var auto by remember(b.settings.flags) { mutableStateOf(b.settings.flags.automatedTrading) }
        var weights by remember(b.settings.flags) { mutableStateOf(b.settings.flags.scannerWeightSet) }
        var reason by remember { mutableStateOf("") }
        var typed by remember { mutableStateOf("") }
        var bio by remember { mutableStateOf(vm.biometricRequired) }
        val turnOn = !b.kill.on                           // the action offered is the opposite of the current state
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(stringResource(R.string.settings_kill_title) + ": " + stringResource(if (b.kill.on) R.string.state_on else R.string.state_off), style = MaterialTheme.typography.titleMedium)
                if (vm.isAdmin) {
                    Text(stringResource(R.string.settings_kill_hint, KillSwitchPhrase.required(turnOn)), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(reason, { reason = it }, label = { Text(stringResource(R.string.settings_reason)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(typed, { typed = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(enabled = reason.isNotBlank() && KillSwitchPhrase.matches(turnOn, typed),
                        onClick = { vm.setKill(turnOn, reason, typed); typed = ""; reason = "" }) {
                        Text(stringResource(if (turnOn) R.string.settings_kill_on else R.string.settings_kill_off))
                    }
                }
            }
            item {
                Text(stringResource(R.string.settings_flags), style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_auto), Modifier.weight(1f))
                    Switch(checked = auto, onCheckedChange = { auto = it }, enabled = vm.isAdmin)
                }
                OutlinedTextField(weights, { weights = it }, label = { Text(stringResource(R.string.settings_weight_set)) }, enabled = vm.isAdmin, modifier = Modifier.fillMaxWidth())
                if (vm.isAdmin) Button(onClick = { vm.saveFlags(Flags(auto, weights.trim())) }) { Text(stringResource(R.string.settings_save)) }
                Text(stringResource(R.string.settings_readonly), style = MaterialTheme.typography.bodySmall)
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_biometric), Modifier.weight(1f))
                    Switch(checked = bio, enabled = biometricAvailable, onCheckedChange = { bio = it; vm.setBiometric(it) })
                }
                if (!biometricAvailable) Text(stringResource(R.string.settings_biometric_unavailable), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.settings_server) + ": " + vm.serverUrl + " (${vm.role})", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onLogout) { Text(stringResource(R.string.logout)) }
            }
        }
    }
}
