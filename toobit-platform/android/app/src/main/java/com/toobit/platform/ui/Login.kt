package com.toobit.platform.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.toobit.platform.R

@Composable
fun LoginScreen(initialUrl: String, busy: Boolean, errorKey: String?, onSubmit: (String, String, String) -> Unit) {
    var url by rememberSaveable { mutableStateOf(initialUrl) }
    var email by rememberSaveable { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }          // never rememberSaveable: do not persist the password in saved state
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp, androidx.compose.ui.Alignment.CenterVertically)) {
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(url, { url = it }, label = { Text(stringResource(R.string.server_url)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
        OutlinedTextField(email, { email = it }, label = { Text(stringResource(R.string.email)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(pw, { pw = it }, label = { Text(stringResource(R.string.password)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        errorKey?.let {
            Text(stringResource(when (it) { "url" -> R.string.url_invalid; "network" -> R.string.network_error; else -> R.string.login_failed }), color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = { onSubmit(url, email, pw) }, enabled = !busy && url.isNotBlank() && email.isNotBlank() && pw.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(stringResource(R.string.sign_in))
        }
    }
}
