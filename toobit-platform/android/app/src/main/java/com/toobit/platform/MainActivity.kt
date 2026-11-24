package com.toobit.platform

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.toobit.platform.ui.*
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val container get() = (application as ToobitApp).container
    private val vm: AppViewModel by viewModels { AppViewModel.Factory(container) }
    private var locked by mutableStateOf(false)
    private val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    private fun biometricAvailable() = BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)   // no screenshots / recents thumbnail
        locked = container.store.biometricRequired && container.store.refreshToken != null
        lifecycleScope.launch { container.restore() }
        setContent { MaterialTheme { Root() } }
    }

    override fun onStop() { super.onStop(); if (container.store.biometricRequired) locked = true }
    override fun onResume() { super.onResume(); if (locked && container.authed.value) promptUnlock() }

    private fun promptUnlock() {
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { locked = false }
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(getString(R.string.lock_title)).setAllowedAuthenticators(authenticators).build())
    }

    @Composable
    private fun Root() {
        val authed by container.authed.collectAsStateWithLifecycle()
        var busy by remember { mutableStateOf(false) }
        var err by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        when {
            !authed -> LoginScreen(container.store.baseUrl.orEmpty(), busy, err) { url, email, pw ->
                busy = true; err = null
                scope.launch { err = container.login(url, email, pw); busy = false }
            }
            locked -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.headlineMedium)
                    Button(onClick = { promptUnlock() }) { Text(stringResource(R.string.lock_unlock)) }
                }
            }
            else -> Main()
        }
    }

    @Composable
    private fun Main() {
        val tabs = listOf(R.string.nav_scanner, R.string.nav_positions, R.string.nav_orders, R.string.nav_signals, R.string.nav_risk, R.string.nav_settings)
        var tab by rememberSaveable { mutableIntStateOf(0) }
        val prices by vm.prices.collectAsStateWithLifecycle()
        val kill by vm.kill.collectAsStateWithLifecycle()
        val snack = remember { SnackbarHostState() }
        LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }
        Scaffold(snackbarHost = { SnackbarHost(snack) }) { inner ->
            Column(Modifier.padding(inner)) {
                (kill as? Ui.Ok)?.v?.takeIf { it.on }?.let { Banner(stringResource(R.string.banner_kill_on), critical = true) }
                (prices as? Ui.Ok)?.v?.let { p ->
                    if (p.marketDataStale) Banner(stringResource(R.string.banner_market_stale), critical = true)
                    if (p.scannerStale) Banner(stringResource(R.string.banner_scanner_stale), critical = false)
                    if (!p.wsConnected) Banner(stringResource(R.string.banner_ws_down), critical = false)
                }
                ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
                    tabs.forEachIndexed { i, r -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(r)) }) }
                }
                when (tab) {
                    0 -> ScannerScreen(vm)
                    1 -> PositionsScreen(vm)
                    2 -> OrdersScreen(vm)
                    3 -> SignalsScreen(vm)
                    4 -> RiskScreen(vm)
                    else -> SettingsScreen(vm, biometricAvailable(), onLogout = { vm.logout() })
                }
            }
        }
    }
}
