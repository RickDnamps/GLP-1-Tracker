package com.rickdnamps.glp1tracker.ui

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.ui.components.HeroCard
import com.rickdnamps.glp1tracker.ui.components.topGlow

/** Biometric / device-credential lock for privacy (health data). */
object AppLock {
    private val authenticators: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_WEAK or DEVICE_CREDENTIAL else BIOMETRIC_WEAK

    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    /** Errors meaning the phone can no longer authenticate: unlock rather than lock the user out. */
    private val unrecoverable = setOf(
        BiometricPrompt.ERROR_HW_NOT_PRESENT,
        BiometricPrompt.ERROR_HW_UNAVAILABLE,
        BiometricPrompt.ERROR_NO_BIOMETRICS,
        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
        BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED,
    )

    fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        cancel: String,
        onUnavailable: () -> Unit = {},
        onSuccess: () -> Unit,
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode in unrecoverable) onUnavailable()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .apply { if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) setNegativeButtonText(cancel) }
            .build()
        prompt.authenticate(info)
    }
}

fun Context.findActivity(): FragmentActivity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is FragmentActivity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun LockScreen(onUnlocked: () -> Unit, onLockUnavailable: () -> Unit) {
    val context = LocalContext.current
    // Fingerprint / screen lock removed since the lock was enabled (or restored from a backup):
    // never trap the user — open the app and turn the lock off.
    if (!AppLock.isAvailable(context)) {
        LaunchedEffect(Unit) { onLockUnavailable() }
        return
    }
    val title = stringResource(R.string.lock_title)
    val subtitle = stringResource(R.string.lock_subtitle)
    val cancel = stringResource(R.string.cancel)
    val unlock = {
        context.findActivity()?.let { AppLock.prompt(it, title, subtitle, cancel, onUnavailable = onLockUnavailable, onSuccess = onUnlocked) }
            ?: onUnlocked()
    }
    LaunchedEffect(Unit) { unlock() }
    Box(
        Modifier
            .fillMaxSize()
            .topGlow(MaterialTheme.colorScheme.primary, com.rickdnamps.glp1tracker.ui.theme.LocalBrand.current.glow)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        HeroCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 24.dp),
            ) {
                Icon(Icons.Outlined.Fingerprint, null, Modifier.size(72.dp), tint = Color.White)
                Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
                Button(onClick = unlock) { Text(stringResource(R.string.lock_unlock)) }
            }
        }
    }
}
