@file:OptIn(ExperimentalMaterial3Api::class)
package com.flolov42.lea_v3.home.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flolov42.lea_v3.home.models.DeviceType
import com.flolov42.lea_v3.home.models.Protocol
import com.flolov42.lea_v3.home.models.SmartDevice
import kotlin.math.*

// ── Étapes du flux ────────────────────────────────────────────────────────────

private enum class AddStep { TypeSelect, RadarScan, Configure }

// ── Entrée principale ─────────────────────────────────────────────────────────

@Composable
fun AddDeviceDialog(
    scanFoundDevices: List<SmartDevice>,
    isScanning: Boolean,
    onSave: (SmartDevice) -> Unit,
    onDismiss: () -> Unit,
    onStartScan: () -> Unit
) {
    var step           by remember { mutableStateOf(AddStep.TypeSelect) }
    var selectedType   by remember { mutableStateOf(DeviceType.LIGHT) }
    var baseDevice     by remember { mutableStateOf<SmartDevice?>(null) }

    // Lance le scan dès l'ouverture
    LaunchedEffect(Unit) { onStartScan() }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .neonGlow(HomeColors.Cyan, glowRadius = 28.dp, cornerRadius = 24.dp, alpha = 0.25f)
                .glassCard(alpha = 0.14f, borderAlpha = 0.36f, cornerRadius = 24.dp)
        ) {
            AnimatedContent(
                targetState   = step,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
                },
                label = "stepAnim"
            ) { currentStep ->
                when (currentStep) {
                    AddStep.TypeSelect -> TypeSelectStep(
                        onTypeSelected = { type ->
                            selectedType = type
                            step = AddStep.RadarScan
                        },
                        onDismiss = onDismiss
                    )
                    AddStep.RadarScan  -> RadarScanStep(
                        deviceType       = selectedType,
                        scanFoundDevices = scanFoundDevices,
                        isScanning       = isScanning,
                        onDeviceSelected = { device ->
                            baseDevice = device
                            step = AddStep.Configure
                        },
                        onManual         = { step = AddStep.Configure },
                        onBack           = { step = AddStep.TypeSelect },
                        onDismiss        = onDismiss,
                        onRetry          = onStartScan
                    )
                    AddStep.Configure  -> ConfigureStep(
                        deviceType = selectedType,
                        baseDevice = baseDevice,
                        onSave     = onSave,
                        onBack     = { step = AddStep.RadarScan },
                        onDismiss  = onDismiss
                    )
                }
            }
        }
    }
}

// ── Étape 1 : Choix du type ───────────────────────────────────────────────────

@Composable
private fun TypeSelectStep(
    onTypeSelected: (DeviceType) -> Unit,
    onDismiss: () -> Unit
) {
    data class Category(val type: DeviceType, val icon: ImageVector, val label: String)

    val categories = listOf(
        Category(DeviceType.LIGHT,        Icons.Filled.LightMode,        "Ampoule"),
        Category(DeviceType.SWITCH,       Icons.Filled.ToggleOn,         "Prise"),
        Category(DeviceType.CAMERA,       Icons.Filled.Videocam,         "Caméra"),
        Category(DeviceType.THERMOSTAT,   Icons.Filled.Thermostat,       "Thermostat"),
        Category(DeviceType.LOCK,         Icons.Filled.Lock,             "Serrure"),
        Category(DeviceType.COVER,        Icons.Filled.Blinds,           "Volet"),
        Category(DeviceType.FAN,          Icons.Filled.Air,              "Ventilateur"),
        Category(DeviceType.MEDIA_PLAYER, Icons.Filled.PlayCircle,       "TV/Enceinte"),
        Category(DeviceType.VACUUM,       Icons.Filled.CleaningServices, "Aspirateur"),
        Category(DeviceType.SENSOR,       Icons.Filled.Sensors,          "Capteur"),
        Category(DeviceType.CLIMATE,      Icons.Filled.AcUnit,           "Climatisation"),
        Category(DeviceType.UNKNOWN,      Icons.Filled.DeviceUnknown,    "Autre")
    )

    Column(
        modifier            = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Quel type d'appareil ?",
                style      = MaterialTheme.typography.titleLarge,
                color      = HomeColors.OnBackground,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            "Choisissez la catégorie de votre appareil connecté",
            style = MaterialTheme.typography.bodyMedium,
            color = HomeColors.OnSurfaceDim
        )

        // Grille de catégories (3 colonnes)
        val rows = categories.chunked(3)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    row.forEach { cat ->
                        CategoryTile(
                            icon    = cat.icon,
                            label   = cat.label,
                            type    = cat.type,
                            onClick = { onTypeSelected(cat.type) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Compléter la dernière ligne si nécessaire
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // Bouton annuler
        TextButton(
            onClick  = onDismiss,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Annuler", color = HomeColors.OnSurfaceDim)
        }
    }
}

@Composable
private fun CategoryTile(
    icon: ImageVector,
    label: String,
    type: DeviceType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = HomeColors.deviceColors[type.name] ?: HomeColors.OnSurfaceDim

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = modifier
            .aspectRatio(1f)
            .neonGlow(color, 6.dp, 14.dp, 0.15f)
            .glassCard(alpha = 0.1f, borderAlpha = 0.2f, cornerRadius = 14.dp)
            .clickableNoRipple { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            fontSize   = 11.sp,
            color      = HomeColors.OnSurface,
            fontWeight = FontWeight.Medium,
            maxLines   = 1
        )
    }
}

// ── Étape 2 : Scan radar ──────────────────────────────────────────────────────

@Composable
private fun RadarScanStep(
    deviceType: DeviceType,
    scanFoundDevices: List<SmartDevice>,
    isScanning: Boolean,
    onDeviceSelected: (SmartDevice) -> Unit,
    onManual: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier            = Modifier
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .heightIn(min = 200.dp, max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.ArrowBack, null, tint = HomeColors.OnSurfaceDim)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                if (isScanning) "Recherche en cours…" else "Appareils trouvés",
                style      = MaterialTheme.typography.titleLarge,
                color      = HomeColors.OnBackground,
                fontWeight = FontWeight.Bold
            )
        }

        // Radar animé
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            RadarAnimation(
                foundCount = scanFoundDevices.size,
                isActive   = isScanning,
                modifier   = Modifier.size(160.dp)
            )
            if (!isScanning && scanFoundDevices.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Aucun appareil détecté", color = HomeColors.OnSurfaceDim, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onRetry) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Réessayer", fontSize = 12.sp)
                    }
                }
            }
        }

        // Liste des appareils trouvés
        if (scanFoundDevices.isNotEmpty()) {
            Text(
                "${scanFoundDevices.size} appareil(s) détecté(s)",
                style = MaterialTheme.typography.labelSmall,
                color = HomeColors.OnSurfaceDim
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier            = Modifier.heightIn(max = 200.dp)
            ) {
                items(scanFoundDevices) { device ->
                    FoundDeviceRow(
                        device  = device,
                        onClick = { onDeviceSelected(device) }
                    )
                }
            }
        }

        HorizontalDivider(color = HomeColors.CardBorder.copy(alpha = 0.18f))

        // Actions bas
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            TextButton(onClick = onManual) {
                Icon(Icons.Filled.Edit, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Saisie manuelle", fontSize = 12.sp)
            }
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = HomeColors.OnSurfaceDim, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FoundDeviceRow(device: SmartDevice, onClick: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .glassCard(alpha = 0.08f, borderAlpha = 0.2f, cornerRadius = 12.dp)
            .clickableNoRipple { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Wifi,
                null,
                tint     = HomeColors.Success,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(device.friendlyName, color = HomeColors.OnBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (!device.ipAddress.isNullOrEmpty()) {
                    Text(device.ipAddress!!, color = HomeColors.OnSurfaceDim, fontSize = 10.sp)
                }
            }
        }
        Icon(Icons.Filled.Add, null, tint = HomeColors.Cyan, modifier = Modifier.size(20.dp))
    }
}

// ── Radar Canvas ──────────────────────────────────────────────────────────────

@Composable
private fun RadarAnimation(
    foundCount: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val sweepTr    = rememberInfiniteTransition(label = "sweep")
    val sweepAngle by sweepTr.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "sw"
    )

    val pulseTr = rememberInfiniteTransition(label = "pulse")
    val ring1   by pulseTr.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2000)), label = "r1"
    )
    val ring2 by pulseTr.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2000, delayMillis = 666)), label = "r2"
    )
    val ring3 by pulseTr.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2000, delayMillis = 1333)), label = "r3"
    )

    val cyanColor   = HomeColors.Cyan
    val cyanDimColor = HomeColors.CyanDim

    Canvas(modifier = modifier) {
        val cx     = size.width / 2f
        val cy     = size.height / 2f
        val radius = size.minDimension / 2f

        // Fond circulaire sombre
        drawCircle(
            color  = Color(0x1A00E5FF),
            radius = radius
        )

        // Anneaux concentriques fixes
        for (i in 1..4) {
            drawCircle(
                color  = cyanColor.copy(alpha = 0.12f),
                radius = radius * i / 4f,
                style  = Stroke(1f)
            )
        }

        // Croix de visée
        val cross = radius * 0.05f
        drawLine(cyanColor.copy(alpha = 0.2f), Offset(cx - cross, cy), Offset(cx + cross, cy), 1f)
        drawLine(cyanColor.copy(alpha = 0.2f), Offset(cx, cy - cross), Offset(cx, cy + cross), 1f)

        if (isActive) {
            // Cône de balayage : secteur en dégradé (4 couches transparence décroissante)
            for (layer in 0..3) {
                val layerAlpha = 0.35f * (1f - layer / 4f)
                val layerAngle = 90f * (1f - layer / 4f)
                rotate(degrees = sweepAngle - layer * 8f, pivot = Offset(cx, cy)) {
                    drawArc(
                        color      = cyanColor.copy(alpha = layerAlpha),
                        startAngle = -90f,
                        sweepAngle = layerAngle,
                        useCenter  = true,
                        topLeft    = Offset(cx - radius, cy - radius),
                        size       = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                    )
                }
            }

            // Ligne de balayage
            rotate(degrees = sweepAngle, pivot = Offset(cx, cy)) {
                drawLine(
                    color       = cyanColor.copy(alpha = 0.85f),
                    start       = Offset(cx, cy),
                    end         = Offset(cx, cy - radius),
                    strokeWidth = 2f
                )
            }

            // Anneaux pulsants
            listOf(ring1, ring2, ring3).forEach { progress ->
                drawCircle(
                    color  = cyanColor.copy(alpha = 0.55f * (1f - progress)),
                    radius = radius * progress,
                    style  = Stroke(2f)
                )
            }
        }

        // Points pour les appareils trouvés (distribution en spirale de Fibonacci)
        repeat(minOf(foundCount, 8)) { i ->
            val angle  = i * 137.508f * PI.toFloat() / 180f
            val r      = radius * (0.25f + (i % 4) * 0.15f)
            val dotX   = cx + r * cos(angle)
            val dotY   = cy + r * sin(angle)
            // Halo
            drawCircle(
                color  = cyanColor.copy(alpha = 0.25f),
                radius = 10f,
                center = Offset(dotX, dotY)
            )
            // Point
            drawCircle(
                color  = cyanColor,
                radius = 5f,
                center = Offset(dotX, dotY)
            )
        }
    }
}

// ── Étape 3 : Configuration ───────────────────────────────────────────────────

@Composable
private fun ConfigureStep(
    deviceType: DeviceType,
    baseDevice: SmartDevice?,
    onSave: (SmartDevice) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    var name      by remember { mutableStateOf(baseDevice?.friendlyName ?: "") }
    var room      by remember { mutableStateOf(baseDevice?.room ?: "Salon") }
    var nameError by remember { mutableStateOf(false) }

    val rooms = listOf(
        "Salon", "Chambre", "Cuisine", "Bureau", "Salle de bain",
        "Entrée", "Garage", "Jardin", "Terrasse", "Général", "Autre"
    )

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor      = HomeColors.Cyan,
        unfocusedBorderColor    = HomeColors.CardBorder.copy(alpha = 0.35f),
        focusedLabelColor       = HomeColors.Cyan,
        unfocusedLabelColor     = HomeColors.OnSurfaceDim,
        cursorColor             = HomeColors.Cyan,
        focusedTextColor        = HomeColors.OnBackground,
        unfocusedTextColor      = HomeColors.OnSurface,
        errorBorderColor        = HomeColors.Error,
        errorLabelColor         = HomeColors.Error,
        focusedContainerColor   = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        errorContainerColor     = Color.Transparent
    )

    Column(
        modifier            = Modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.ArrowBack, null, tint = HomeColors.OnSurfaceDim)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "Configurer l'appareil",
                style      = MaterialTheme.typography.titleLarge,
                color      = HomeColors.OnBackground,
                fontWeight = FontWeight.Bold
            )
        }

        // Type badge
        val typeColor = HomeColors.deviceColors[deviceType.name] ?: HomeColors.OnSurfaceDim
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier
                .clip(RoundedCornerShape(50))
                .background(typeColor.copy(alpha = 0.12f))
                .border(1.dp, typeColor.copy(alpha = 0.3f), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                deviceType.name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " "),
                color    = typeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Champ nom
        OutlinedTextField(
            value          = name,
            onValueChange  = { name = it; nameError = false },
            label          = { Text("Nom de l'appareil *") },
            placeholder    = { Text("Ex: Lumière salon", color = HomeColors.OnSurfaceDim.copy(alpha = 0.5f)) },
            colors         = fieldColors,
            isError        = nameError,
            supportingText = if (nameError) {
                { Text("Le nom est requis", color = HomeColors.Error, fontSize = 11.sp) }
            } else null,
            modifier       = Modifier.fillMaxWidth(),
            singleLine     = true,
            shape          = RoundedCornerShape(12.dp)
        )

        // Choix de la pièce (grille de chips)
        Text("Pièce", style = MaterialTheme.typography.labelSmall, color = HomeColors.OnSurfaceDim)
        RoomChipsGrid(
            rooms    = rooms,
            selected = room,
            onSelect = { room = it }
        )

        HorizontalDivider(color = HomeColors.CardBorder.copy(alpha = 0.18f))

        // Boutons
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(12.dp),
                border   = androidx.compose.foundation.BorderStroke(
                    1.dp, HomeColors.CardBorder.copy(alpha = 0.3f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = HomeColors.OnSurfaceDim)
            ) {
                Text("ANNULER", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Button(
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }

                    val entityId = baseDevice?.entityId
                        ?: "local_${name.trim().replace(" ", "_").lowercase()}_${System.currentTimeMillis()}"

                    val device = SmartDevice(entityId, name.trim(), deviceType, Protocol.LOCAL_HTTP, room)
                    device.ipAddress = baseDevice?.ipAddress
                    device.state     = "unknown"
                    onSave(device)
                },
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = HomeColors.CyanDim,
                    contentColor   = HomeColors.Cyan
                )
            ) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("AJOUTER", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun RoomChipsGrid(
    rooms: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val rows = rooms.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { r ->
                    val isSelected = r == selected
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) HomeColors.CyanDim.copy(alpha = 0.5f)
                                else HomeColors.CardGlass
                            )
                            .border(
                                1.dp,
                                if (isSelected) HomeColors.Cyan.copy(alpha = 0.6f)
                                else HomeColors.CardBorder.copy(alpha = 0.2f),
                                RoundedCornerShape(50)
                            )
                            .clickableNoRipple { onSelect(r) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            r,
                            fontSize   = 12.sp,
                            color      = if (isSelected) HomeColors.Cyan else HomeColors.OnSurface,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
