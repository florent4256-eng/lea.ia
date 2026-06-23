package com.flolov42.lea_v3.home.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flolov42.lea_v3.home.database.LeaHomeDatabase
import com.flolov42.lea_v3.home.models.SmartDevice
import kotlinx.coroutines.launch

// ── Navigation ────────────────────────────────────────────────────────────────

sealed class LscTab {
    object Devices  : LscTab()
    object Rooms    : LscTab()
    object Chat     : LscTab()
    object Settings : LscTab()
}

// ── State ─────────────────────────────────────────────────────────────────────

sealed class HomeUiState {
    object Idle        : HomeUiState()
    object Discovering : HomeUiState()
    data class Error(val msg: String) : HomeUiState()
}

// ── Main Screen ───────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    devices: List<SmartDevice>,
    scenes: List<LeaHomeDatabase.Scene>,
    rooms: List<String>,
    selectedRoom: String?,
    showFavorites: Boolean,
    uiState: HomeUiState,
    chatMessages: List<ChatMessage>,
    isListening: Boolean,
    wsConnected: Boolean,
    scanFoundDevices: List<SmartDevice>,
    onRoomSelected: (String?) -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleDevice: (SmartDevice) -> Unit,
    onLongPressDevice: (SmartDevice) -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onSendCommand: (String) -> Unit,
    onMicPress: () -> Unit,
    onAddDevice: (SmartDevice) -> Unit,
    onActivateScene: (LeaHomeDatabase.Scene) -> Unit,
    onCreateScene: (String) -> Unit,
    onDeleteScene: (Long) -> Unit,
    onSetBrightness: (SmartDevice, Int) -> Unit,
    onSetTemperature: (SmartDevice, Float) -> Unit,
    onCoverOpen: (SmartDevice) -> Unit,
    onCoverClose: (SmartDevice) -> Unit,
    onCoverStop: (SmartDevice) -> Unit
) {
    var activeTab   by remember { mutableStateOf<LscTab>(LscTab.Devices) }
    var showAddFlow by remember { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope        = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeColors.Background)
    ) {
        AmbientGlow()

        Column(modifier = Modifier.fillMaxSize()) {

            LscTopBar(
                uiState     = uiState,
                wsConnected = wsConnected,
                onSettings  = onSettings,
                onRefresh   = onRefresh
            )

            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    is LscTab.Devices -> DevicesTab(
                        devices           = devices,
                        scenes            = scenes,
                        rooms             = rooms,
                        selectedRoom      = selectedRoom,
                        showFavorites     = showFavorites,
                        uiState           = uiState,
                        onRoomSelected    = onRoomSelected,
                        onToggleFavorites = onToggleFavorites,
                        onToggleDevice    = onToggleDevice,
                        onLongPressDevice = onLongPressDevice,
                        onRefresh         = onRefresh,
                        onAddFirst        = { onRefresh(); showAddFlow = true },
                        onActivateScene   = onActivateScene,
                        onCreateScene     = onCreateScene,
                        onDeleteScene     = onDeleteScene,
                        onSetBrightness   = onSetBrightness,
                        onSetTemperature  = onSetTemperature,
                        onCoverOpen       = onCoverOpen,
                        onCoverClose      = onCoverClose,
                        onCoverStop       = onCoverStop
                    )
                    is LscTab.Rooms -> RoomsOverview(
                        rooms    = rooms,
                        devices  = devices,
                        onSelect = { room ->
                            onRoomSelected(room)
                            activeTab = LscTab.Devices
                        }
                    )
                    is LscTab.Chat -> LeaChatPanel(
                        messages      = chatMessages,
                        onSendCommand = onSendCommand,
                        onMicPress    = onMicPress,
                        isListening   = isListening,
                        modifier      = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )
                    is LscTab.Settings -> {}
                }
            }

            LscBottomNav(
                active     = activeTab,
                onTab      = { tab ->
                    if (tab is LscTab.Settings) onSettings()
                    else activeTab = tab
                },
                onAddClick = { onRefresh(); showAddFlow = true }
            )
        }

        SnackbarHost(
            hostState = snackbarHost,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }

    if (showAddFlow) {
        AddDeviceDialog(
            scanFoundDevices = scanFoundDevices,
            isScanning       = uiState is HomeUiState.Discovering,
            onSave           = { device ->
                onAddDevice(device)
                showAddFlow = false
                scope.launch { snackbarHost.showSnackbar("${device.friendlyName} ajouté ✓") }
            },
            onDismiss   = { showAddFlow = false },
            onStartScan = onRefresh
        )
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@Composable
fun LscTopBar(
    uiState: HomeUiState,
    wsConnected: Boolean,
    onSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    val refreshAnim = rememberInfiniteTransition(label = "refresh")
    val refreshRot  by refreshAnim.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "rr"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        HomeColors.Surface.copy(alpha = 0.95f),
                        HomeColors.Background.copy(alpha = 0f)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Mes appareils",
                style      = MaterialTheme.typography.headlineMedium,
                color      = HomeColors.OnBackground,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (wsConnected) HomeColors.Success else HomeColors.StateOff)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = when (uiState) {
                        is HomeUiState.Discovering -> "Scan en cours…"
                        is HomeUiState.Error       -> "Erreur"
                        else -> if (wsConnected) "Connecté" else "Local"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = HomeColors.OnSurfaceDim
                )
            }
        }

        IconButton(onClick = onRefresh) {
            Icon(
                Icons.Filled.Refresh,
                null,
                tint     = HomeColors.OnSurfaceDim,
                modifier = if (uiState is HomeUiState.Discovering) Modifier.rotate(refreshRot) else Modifier
            )
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, null, tint = HomeColors.OnSurfaceDim)
        }
    }
}

// ── Onglet Appareils ──────────────────────────────────────────────────────────

@Composable
private fun DevicesTab(
    devices: List<SmartDevice>,
    scenes: List<LeaHomeDatabase.Scene>,
    rooms: List<String>,
    selectedRoom: String?,
    showFavorites: Boolean,
    uiState: HomeUiState,
    onRoomSelected: (String?) -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleDevice: (SmartDevice) -> Unit,
    onLongPressDevice: (SmartDevice) -> Unit,
    onRefresh: () -> Unit,
    onAddFirst: () -> Unit,
    onActivateScene: (LeaHomeDatabase.Scene) -> Unit,
    onCreateScene: (String) -> Unit,
    onDeleteScene: (Long) -> Unit,
    onSetBrightness: (SmartDevice, Int) -> Unit,
    onSetTemperature: (SmartDevice, Float) -> Unit,
    onCoverOpen: (SmartDevice) -> Unit,
    onCoverClose: (SmartDevice) -> Unit,
    onCoverStop: (SmartDevice) -> Unit
) {
    var showCreateSceneDialog by remember { mutableStateOf(false) }
    var pendingDeleteScene by remember { mutableStateOf<LeaHomeDatabase.Scene?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {

        RoomTabBar(
            rooms             = rooms,
            selectedRoom      = selectedRoom,
            showFavorites     = showFavorites,
            onRoomSelected    = onRoomSelected,
            onToggleFavorites = onToggleFavorites
        )

        SceneSection(
            scenes          = scenes,
            onActivate      = onActivateScene,
            onLongPress     = { pendingDeleteScene = it },
            onAddClick      = { showCreateSceneDialog = true }
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState is HomeUiState.Discovering && devices.isEmpty() -> LoadingPlaceholder()
                devices.isEmpty() -> EmptyStateLsc(onAddFirst)
                else -> DeviceGrid(
                    devices          = devices,
                    onToggle         = onToggleDevice,
                    onLongPress      = onLongPressDevice,
                    onSetBrightness  = onSetBrightness,
                    onSetTemperature = onSetTemperature,
                    onCoverOpen      = onCoverOpen,
                    onCoverClose     = onCoverClose,
                    onCoverStop      = onCoverStop
                )
            }
            if (uiState is HomeUiState.Error) {
                ErrorBanner(msg = uiState.msg, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }

    // Create scene dialog
    if (showCreateSceneDialog) {
        CreateSceneDialog(
            onConfirm = { name -> onCreateScene(name); showCreateSceneDialog = false },
            onDismiss = { showCreateSceneDialog = false }
        )
    }

    // Delete scene confirmation
    pendingDeleteScene?.let { scene ->
        AlertDialog(
            onDismissRequest = { pendingDeleteScene = null },
            containerColor   = HomeColors.Surface,
            title   = { Text("Supprimer « ${scene.name} » ?", color = HomeColors.OnBackground) },
            text    = { Text("Cette scène sera supprimée définitivement.", color = HomeColors.OnSurfaceDim) },
            confirmButton = {
                TextButton(onClick = { onDeleteScene(scene.id); pendingDeleteScene = null }) {
                    Text("Supprimer", color = HomeColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteScene = null }) {
                    Text("Annuler", color = HomeColors.OnSurfaceDim)
                }
            }
        )
    }
}

// ── Section scènes ────────────────────────────────────────────────────────────

@Composable
private fun SceneSection(
    scenes: List<LeaHomeDatabase.Scene>,
    onActivate: (LeaHomeDatabase.Scene) -> Unit,
    onLongPress: (LeaHomeDatabase.Scene) -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 4.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Scènes",
                style         = MaterialTheme.typography.labelSmall,
                color         = HomeColors.OnSurfaceDim,
                fontSize      = 10.sp,
                letterSpacing = 1.sp
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(HomeColors.CyanDim.copy(alpha = 0.3f))
                    .clickableNoRipple { onAddClick() }
            ) {
                Icon(Icons.Filled.Add, null, tint = HomeColors.Cyan, modifier = Modifier.size(14.dp))
            }
        }

        if (scenes.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding        = PaddingValues(horizontal = 0.dp)
            ) {
                items(scenes, key = { it.id }) { scene ->
                    ScenePill(
                        scene      = scene,
                        onActivate = { onActivate(scene) },
                        onLongPress = { onLongPress(scene) }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScenePill(
    scene: LeaHomeDatabase.Scene,
    onActivate: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .neonGlow(HomeColors.Violet, glowRadius = 6.dp, cornerRadius = 50.dp, alpha = 0.3f)
            .clip(RoundedCornerShape(50))
            .background(HomeColors.VioletDim.copy(alpha = 0.25f))
            .border(1.dp, HomeColors.Violet.copy(alpha = 0.35f), RoundedCornerShape(50))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onActivate,
                onLongClick       = onLongPress
            )
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(scene.icon ?: "✨", fontSize = 12.sp)
            Text(
                scene.name,
                color      = HomeColors.OnSurface,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Dialog créer scène ────────────────────────────────────────────────────────

@Composable
private fun CreateSceneDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth(0.9f)
                .neonGlow(HomeColors.Violet, 20.dp, 20.dp, 0.2f)
                .glassCard(alpha = 0.14f, borderAlpha = 0.35f, cornerRadius = 20.dp)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✨", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Nouvelle scène",
                    style      = MaterialTheme.typography.titleLarge,
                    color      = HomeColors.OnBackground,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "La scène sauvegardera l'état actuel de tous les appareils allumés.",
                style = MaterialTheme.typography.bodyMedium,
                color = HomeColors.OnSurfaceDim
            )
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it; error = false },
                label         = { Text("Nom de la scène") },
                isError       = error,
                supportingText = if (error) {
                    { Text("Le nom est requis", color = HomeColors.Error, fontSize = 11.sp) }
                } else null,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = HomeColors.Violet,
                    unfocusedBorderColor = HomeColors.CardBorder.copy(alpha = 0.35f),
                    focusedLabelColor    = HomeColors.Violet,
                    unfocusedLabelColor  = HomeColors.OnSurfaceDim,
                    cursorColor          = HomeColors.Violet,
                    focusedTextColor     = HomeColors.OnBackground,
                    unfocusedTextColor   = HomeColors.OnSurface,
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    errorContainerColor     = Color.Transparent
                ),
                singleLine = true,
                shape      = RoundedCornerShape(12.dp),
                modifier   = Modifier.fillMaxWidth()
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, HomeColors.CardBorder.copy(alpha = 0.3f)),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = HomeColors.OnSurfaceDim)
                ) {
                    Text("ANNULER", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Button(
                    onClick = {
                        if (name.isBlank()) { error = true; return@Button }
                        onConfirm(name.trim())
                    },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = HomeColors.VioletDim,
                        contentColor   = HomeColors.Violet
                    )
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("CRÉER", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}

// ── Onglet Pièces ─────────────────────────────────────────────────────────────

@Composable
private fun RoomsOverview(
    rooms: List<String>,
    devices: List<SmartDevice>,
    onSelect: (String?) -> Unit
) {
    if (rooms.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucune pièce configurée", color = HomeColors.OnSurfaceDim)
        }
        return
    }

    LazyVerticalGrid(
        columns               = GridCells.Fixed(2),
        contentPadding        = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement   = Arrangement.spacedBy(12.dp),
        modifier              = Modifier.fillMaxSize()
    ) {
        items(rooms) { room ->
            val count   = devices.count { it.room == room }
            val onCount = devices.count { it.room == room && it.isOn() }
            RoomCard(room = room, total = count, on = onCount, onClick = { onSelect(room) })
        }
    }
}

@Composable
private fun RoomCard(room: String, total: Int, on: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.3f)
            .neonGlow(HomeColors.Cyan, 8.dp, 16.dp, if (on > 0) 0.25f else 0f)
            .glassCard(alpha = 0.1f, borderAlpha = 0.25f, cornerRadius = 16.dp)
            .clickableNoRipple { onClick() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.MeetingRoom, null, tint = HomeColors.Cyan, modifier = Modifier.size(28.dp))
            Text(room, color = HomeColors.OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                "$total appareil${if (total > 1) "s" else ""}${if (on > 0) " · $on allumé${if (on > 1) "s" else ""}" else ""}",
                color    = HomeColors.OnSurfaceDim,
                fontSize = 11.sp
            )
        }
    }
}

// ── Device Grid ───────────────────────────────────────────────────────────────

@Composable
private fun DeviceGrid(
    devices: List<SmartDevice>,
    onToggle: (SmartDevice) -> Unit,
    onLongPress: (SmartDevice) -> Unit,
    onSetBrightness: (SmartDevice, Int) -> Unit,
    onSetTemperature: (SmartDevice, Float) -> Unit,
    onCoverOpen: (SmartDevice) -> Unit,
    onCoverClose: (SmartDevice) -> Unit,
    onCoverStop: (SmartDevice) -> Unit
) {
    LazyVerticalGrid(
        columns               = GridCells.Adaptive(minSize = 150.dp),
        contentPadding        = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement   = Arrangement.spacedBy(10.dp),
        modifier              = Modifier.fillMaxSize()
    ) {
        items(devices, key = { it.entityId }) { device ->
            AnimatedVisibility(
                visible = true,
                enter   = fadeIn(tween(300)) + scaleIn(tween(300, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)))
            ) {
                DeviceCard(
                    device           = device,
                    onToggle         = { onToggle(device) },
                    onLongPress      = { onLongPress(device) },
                    onSetBrightness  = { pct  -> onSetBrightness(device, pct) },
                    onSetTemperature = { temp -> onSetTemperature(device, temp) },
                    onCoverOpen      = { onCoverOpen(device) },
                    onCoverClose     = { onCoverClose(device) },
                    onCoverStop      = { onCoverStop(device) },
                    modifier         = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ── Bottom Navigation ─────────────────────────────────────────────────────────

@Composable
fun LscBottomNav(
    active: LscTab,
    onTab: (LscTab) -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter)
                .background(HomeColors.Surface.copy(alpha = 0.97f))
                .border(
                    width = 1.dp,
                    color = HomeColors.CardBorder.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavTabItem(icon = Icons.Filled.Home,     label = "Appareils", active = active is LscTab.Devices,  onClick = { onTab(LscTab.Devices) })
            NavTabItem(icon = Icons.Filled.GridView, label = "Pièces",    active = active is LscTab.Rooms,    onClick = { onTab(LscTab.Rooms) })
            Spacer(Modifier.width(56.dp))
            NavTabItem(icon = Icons.Filled.Chat,     label = "Chat",      active = active is LscTab.Chat,     onClick = { onTab(LscTab.Chat) })
            NavTabItem(icon = Icons.Filled.Settings, label = "Réglages",  active = active is LscTab.Settings, onClick = { onTab(LscTab.Settings) })
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .align(Alignment.TopCenter)
                .size(54.dp)
                .neonGlow(HomeColors.Cyan, glowRadius = 14.dp, cornerRadius = 27.dp, alpha = 0.65f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(HomeColors.CyanDim.copy(alpha = 0.85f), HomeColors.Cyan.copy(alpha = 0.6f))
                    )
                )
                .border(2.dp, HomeColors.Cyan.copy(alpha = 0.75f), CircleShape)
                .clickableNoRipple { onAddClick() }
        ) {
            Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun NavTabItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue   = if (active) HomeColors.Cyan else HomeColors.OnSurfaceDim,
        animationSpec = tween(200),
        label         = "navColor"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier
            .clickableNoRipple { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            fontSize   = 9.sp,
            color      = tint,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyStateLsc(onAdd: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(96.dp)
                    .neonGlow(HomeColors.Cyan, 18.dp, 48.dp, 0.2f)
                    .clip(CircleShape)
                    .background(HomeColors.CyanDim.copy(alpha = 0.12f))
            ) {
                Icon(Icons.Filled.HomeWork, null, tint = HomeColors.Cyan.copy(alpha = 0.6f), modifier = Modifier.size(52.dp))
            }
            Text(
                "Aucun appareil ajouté",
                style      = MaterialTheme.typography.titleMedium,
                color      = HomeColors.OnBackground,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Ajoutez vos ampoules, prises et caméras\npour les contrôler depuis ici",
                style     = MaterialTheme.typography.bodyMedium,
                color     = HomeColors.OnSurfaceDim,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onAdd,
                shape   = RoundedCornerShape(50),
                colors  = ButtonDefaults.buttonColors(
                    containerColor = HomeColors.Cyan,
                    contentColor   = HomeColors.Background
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ajouter un appareil", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun LoadingPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PulsingLoader(color = HomeColors.Cyan, modifier = Modifier.size(72.dp))
            Text("Recherche des appareils…", style = MaterialTheme.typography.bodyMedium, color = HomeColors.OnSurfaceDim)
        }
    }
}

// ── Error banner ──────────────────────────────────────────────────────────────

@Composable
private fun ErrorBanner(msg: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(HomeColors.Error.copy(alpha = 0.15f))
            .border(1.dp, HomeColors.Error.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, null, tint = HomeColors.Error, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(msg, color = HomeColors.Error, fontSize = 12.sp)
        }
    }
}

// ── Ambient glow ──────────────────────────────────────────────────────────────

@Composable
private fun AmbientGlow() {
    val anim  = rememberInfiniteTransition(label = "ambient")
    val shift by anim.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(8000, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)), RepeatMode.Reverse),
        label = "sh"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(HomeColors.Cyan.copy(alpha = 0.04f + shift * 0.02f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(0.2f, 0.1f),
                    radius = 800f
                )
            )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(HomeColors.Violet.copy(alpha = 0.04f + (1f - shift) * 0.02f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(0.8f, 0.9f),
                    radius = 700f
                )
            )
    )
}
