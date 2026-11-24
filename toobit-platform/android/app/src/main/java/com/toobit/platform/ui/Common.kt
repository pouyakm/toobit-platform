package com.toobit.platform.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.toobit.platform.R
import java.util.Locale

fun fmt(v: Double, d: Int = 4): String = String.format(Locale.US, "%.${d}f", v)

/** Renders loading / error / empty / data uniformly. Stale data keeps showing with a hint. */
@Composable
fun <T> UiBox(state: Ui<T>, isEmpty: (T) -> Boolean = { false }, content: @Composable (T) -> Unit) {
    when (state) {
        Ui.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        is Ui.Fail -> Box(Modifier.fillMaxSize().padding(16.dp), Alignment.Center) {
            Text(stringResource(R.string.error) + " (" + state.msg + ")", color = MaterialTheme.colorScheme.error)
        }
        is Ui.Ok -> if (isEmpty(state.v)) Box(Modifier.fillMaxSize(), Alignment.Center) { Text(stringResource(R.string.none)) }
        else Column {
            if (state.failed) Text(stringResource(R.string.network_error), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            content(state.v)
        }
    }
}

@Composable
fun Banner(text: String, critical: Boolean) {
    Surface(color = if (critical) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ConfirmDialog(title: String, body: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
