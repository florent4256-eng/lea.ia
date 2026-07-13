package com.flolov42.lea_v3.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.flolov42.lea_v3.home.control.DeviceControlManager
import com.flolov42.lea_v3.home.database.LeaHomeDatabase
import com.flolov42.lea_v3.home.discovery.DeviceDiscoveryService
import com.flolov42.lea_v3.home.models.SmartDevice
import com.flolov42.lea_v3.home.network.LeaHomeWebSocketListener
import com.flolov42.lea_v3.home.ui.*
import com.flolov42.lea_v3.home.voice.LeaHomeVoiceProcessor
import com.flolov42.lea_v3.voice.LeaVoiceCommandManager

class LeaHomeActivity : AppCompatActivity() {

    private val viewModel: HomeViewModel by viewModels()

    private val requestAudio = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startListening() }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
        super.onCreate(savedInstanceState)

        if (!viewModel.servicesReady) {
            with(viewModel) {
                db             = LeaHomeDatabase.get(this@LeaHomeActivity)
                control        = DeviceControlManager.get(this@LeaHomeActivity)
                discovery      = DeviceDiscoveryService(this@LeaHomeActivity)
                wsListener     = LeaHomeWebSocketListener(this@LeaHomeActivity)
                voiceProcessor = LeaHomeVoiceProcessor(this@LeaHomeActivity)
                servicesReady  = true
            }
            viewModel.init()
            viewModel.startNetworkScan()
        }

        setContent {
            LeaHomeTheme {
                LeaHomeRoot(
                    vm         = viewModel,
                    onSettings = { openSettings() },
                    onMicPress = { handleMicPress() },
                    onBack     = { finish() }
                )
            }
        }

        // Fullscreen immersif — status bar + nav bar cachées
        window.decorView.post { setImmersiveMode() }
        window.decorView.postDelayed({ setImmersiveMode() }, 500)

        } catch (e: Throwable) {
            val el = e.stackTrace.firstOrNull { it.className.contains("lea_v3") }
            val loc = if (el != null) "${el.fileName}:${el.lineNumber}" else "?"
            android.widget.Toast.makeText(this, "❌ Home: ${e.message}\n@ $loc", android.widget.Toast.LENGTH_LONG).show()
            com.flolov42.lea_v3.utilities.LeaAndroidLogger.crash(this, "Home onCreate", e)
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) setImmersiveMode()
    }

    @Suppress("DEPRECATION")
    private fun setImmersiveMode() {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { ctrl ->
                ctrl.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                ctrl.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }

    private fun openSettings() {
        HomeAssistantSetupDialog.show(this, object : HomeAssistantSetupDialog.SetupCallback {
            override fun onSaved()     { viewModel.startDiscovery() }
            override fun onCancelled() {}
        })
    }

    fun startListening() {
        viewModel.setListening(true)
        LeaVoiceCommandManager.get(this).startListening(object : LeaVoiceCommandManager.VoiceCallback {
            override fun onResult(command: String, response: String) {
                viewModel.setListening(false)
                viewModel.processVoiceCommand(command)
            }
            override fun onError(error: String) {
                viewModel.setListening(false)
            }
        })
    }

    private fun handleMicPress() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestAudio.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startListening()
        }
    }
}

// ── Top-level composable (sans contexte Activity capturé) ─────────────────────

@Composable
fun LeaHomeRoot(
    vm: HomeViewModel,
    onSettings: () -> Unit,
    onMicPress: () -> Unit,
    onBack: () -> Unit = {}
) {
    val devices          by vm.devices.collectAsState()
    val rooms            by vm.rooms.collectAsState()
    val selectedRoom     by vm.selectedRoom.collectAsState()
    val uiState          by vm.uiState.collectAsState()
    val messages         by vm.messages.collectAsState()
    val isListening      by vm.isListening.collectAsState()
    val wsConnected      by vm.wsConnected.collectAsState()
    val scanFoundDevices by vm.scanFoundDevices.collectAsState()
    val scenes           by vm.scenes.collectAsState()
    val showFavorites    by vm.showFavorites.collectAsState()

    var longPressDevice by remember { mutableStateOf<SmartDevice?>(null) }

    HomeScreen(
        devices          = devices,
        scenes           = scenes,
        rooms            = rooms,
        selectedRoom     = selectedRoom,
        showFavorites    = showFavorites,
        uiState          = uiState,
        chatMessages     = messages,
        isListening      = isListening,
        wsConnected      = wsConnected,
        scanFoundDevices = scanFoundDevices,
        onRoomSelected   = { vm.selectRoom(it) },
        onToggleFavorites = { vm.toggleFavoriteFilter() },
        onToggleDevice   = { vm.toggleDevice(it) },
        onLongPressDevice = { longPressDevice = it },
        onRefresh        = { vm.startNetworkScan() },
        onSettings       = onSettings,
        onSendCommand    = { vm.processVoiceCommand(it) },
        onMicPress       = onMicPress,
        onAddDevice      = { vm.addDevice(it) },
        onActivateScene  = { vm.activateLocalScene(it) },
        onCreateScene    = { vm.createScene(it) },
        onDeleteScene    = { vm.deleteScene(it) },
        onSetBrightness  = { device, pct  -> vm.setBrightness(device, pct) },
        onSetTemperature = { device, temp -> vm.setTemperature(device, temp) },
        onCoverOpen      = { vm.openCover(it) },
        onCoverClose     = { vm.closeCover(it) },
        onCoverStop      = { vm.stopCover(it) },
        onBack           = onBack
    )

    longPressDevice?.let { d ->
        LeaDeviceMenu(
            device          = d,
            onDismiss       = { longPressDevice = null },
            onTurnOn        = { vm.toggleDevice(d); longPressDevice = null },
            onTurnOff       = { vm.toggleDevice(d); longPressDevice = null },
            onToggleFavorite = { vm.toggleFavoriteDevice(d); longPressDevice = null },
            onDelete        = { vm.deleteDevice(d.entityId); longPressDevice = null }
        )
    }
}

@Composable
private fun LeaDeviceMenu(
    device: SmartDevice,
    onDismiss: () -> Unit,
    onTurnOn: () -> Unit,
    onTurnOff: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val favLabel = if (device.isFavorite) "Retirer des favoris" else "Ajouter aux favoris"
    val favColor = HomeColors.deviceColors["LOCK"] ?: HomeColors.Cyan

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = HomeColors.Surface,
        title  = { Text(device.friendlyName, color = HomeColors.OnBackground) },
        text   = {
            androidx.compose.foundation.layout.Column {
                HomeMenuAction("Allumer",     HomeColors.Success, onTurnOn)
                HomeMenuAction("Éteindre",    HomeColors.StateOff, onTurnOff)
                HomeMenuAction(favLabel,      favColor,            onToggleFavorite)
                HomeMenuAction("Supprimer",   HomeColors.Error,    onDelete)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = HomeColors.OnSurfaceDim)
            }
        }
    )
}

@Composable
private fun HomeMenuAction(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = color)
    }
}
