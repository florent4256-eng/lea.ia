package com.flolov42.lea_v3.home.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flolov42.lea_v3.home.models.DeviceType
import com.flolov42.lea_v3.home.models.SmartDevice

@Composable
fun DeviceCard(
    device: SmartDevice,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onSetBrightness: ((Int) -> Unit)? = null,
    onSetTemperature: ((Float) -> Unit)? = null,
    onCoverOpen: (() -> Unit)? = null,
    onCoverClose: (() -> Unit)? = null,
    onCoverStop: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isOn        = device.isOn()
    val isAvailable = device.isAvailable()
    val deviceColor = deviceColor(device.type)

    val isSensor  = device.type == DeviceType.SENSOR
    val isCamera  = device.type == DeviceType.CAMERA
    val isCover   = device.type == DeviceType.COVER
    val isClimate = device.type == DeviceType.CLIMATE || device.type == DeviceType.THERMOSTAT
    val isLight   = device.type == DeviceType.LIGHT

    // Parse attributes once per recomposition triggered by state change
    val brightness   = remember(device.attributes) { device.attrInt("brightness")?.let { (it / 2.55f).toInt().coerceIn(0, 100) } }
    val targetTemp   = remember(device.attributes) { device.attrFloat("temperature") }
    val currentTemp  = remember(device.attributes) { device.attrFloat("current_temperature") }
    val coverPos     = remember(device.attributes) { device.attrInt("current_position") }
    val sensorUnit   = remember(device.attributes) { device.attrString("unit_of_measurement") }

    // Slider local state — committed on release only
    var sliderBright by remember(brightness) { mutableStateOf(brightness?.toFloat() ?: 0f) }

    // ── Animations ────────────────────────────────────────────────────────────
    val glowAlpha by animateFloatAsState(
        targetValue   = if ((isOn || isSensor) && isAvailable) 0.7f else 0f,
        animationSpec = tween(400, easing = EaseInOutQuad),
        label = "glow"
    )
    val cardBackground by animateColorAsState(
        targetValue   = if (isOn || isSensor) deviceColor.copy(alpha = 0.13f) else HomeColors.CardGlass,
        animationSpec = tween(350),
        label = "bg"
    )
    val iconScale by animateFloatAsState(
        targetValue   = if (isOn) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "icon"
    )
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue  = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "ps"
    )
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue   = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "press"
    )

    Box(
        modifier = modifier
            .scale(pressScale)
            .then(
                if (glowAlpha > 0f) Modifier.neonGlow(deviceColor, glowRadius = 14.dp, alpha = glowAlpha * 0.55f)
                else Modifier
            )
            .glassCard(cornerRadius = 20.dp)
            .background(cardBackground)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap       = { if (!isSensor && !isCamera) onToggle() },
                    onLongPress = { onLongPress() }
                )
            }
            .padding(12.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // ── Icon circle ───────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .scale(if (isOn || isSensor) iconScale * pulseScale else iconScale)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOn || isSensor) deviceColor.copy(alpha = 0.2f) else HomeColors.CardGlass
                    )
            ) {
                if (isOn || isSensor) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .neonGlow(deviceColor, glowRadius = 10.dp, alpha = 0.5f)
                    )
                }
                Icon(
                    imageVector    = iconFor(device.type),
                    contentDescription = null,
                    tint           = if (isOn || isSensor) deviceColor else HomeColors.OnSurfaceDim,
                    modifier       = Modifier.size(22.dp)
                )
            }

            // ── Device name ───────────────────────────────────────────────────
            Text(
                text      = device.friendlyName,
                style     = MaterialTheme.typography.titleMedium,
                color     = if (isAvailable) HomeColors.OnBackground else HomeColors.OnSurfaceDim,
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis,
                fontSize  = 12.sp,
                modifier  = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // ── State label (type-specific) ───────────────────────────────────
            when {
                isSensor -> {
                    Text(
                        text       = "${device.state}${sensorUnit?.let { " $it" } ?: ""}",
                        color      = deviceColor,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                }
                isCamera -> {
                    val camLabel = when (device.state.lowercase()) {
                        "recording" -> "Enregistrement"
                        "streaming" -> "Streaming"
                        "idle"      -> "En veille"
                        "on"        -> "En ligne"
                        else        -> "Hors ligne"
                    }
                    val camOn = device.state in listOf("recording", "streaming", "on")
                    Text(
                        text          = camLabel,
                        style         = MaterialTheme.typography.labelSmall,
                        color         = if (camOn) deviceColor else HomeColors.StateOff,
                        fontSize      = 9.sp,
                        letterSpacing = 1.sp
                    )
                }
                isClimate -> {
                    if (currentTemp != null) {
                        Text(
                            text       = "${"%.1f".format(currentTemp)} °C",
                            color      = deviceColor,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 16.sp
                        )
                    } else {
                        Text(
                            text          = if (isOn) "ALLUMÉ" else "ÉTEINT",
                            style         = MaterialTheme.typography.labelSmall,
                            color         = if (isOn) deviceColor else HomeColors.StateOff,
                            fontSize      = 9.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
                isCover -> {
                    val coverLabel = when (device.state.lowercase()) {
                        "open"    -> "OUVERT"
                        "closed"  -> "FERMÉ"
                        "opening" -> "Ouverture…"
                        "closing" -> "Fermeture…"
                        else      -> device.state.uppercase()
                    }
                    val coverOpen = device.state.lowercase() in listOf("open", "opening")
                    Text(
                        text          = coverLabel + (coverPos?.let { " · $it%" } ?: ""),
                        style         = MaterialTheme.typography.labelSmall,
                        color         = if (coverOpen) deviceColor else HomeColors.StateOff,
                        fontSize      = 9.sp,
                        letterSpacing = 1.sp
                    )
                }
                else -> {
                    Text(
                        text = when {
                            !isAvailable -> "Non dispo"
                            isOn         -> "ALLUMÉ"
                            else         -> "ÉTEINT"
                        },
                        style         = MaterialTheme.typography.labelSmall,
                        color         = when {
                            !isAvailable -> HomeColors.OnSurfaceDim
                            isOn         -> deviceColor
                            else         -> HomeColors.StateOff
                        },
                        fontSize      = 9.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            // ── Controls (type-specific) ──────────────────────────────────────
            when {
                isCover -> {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        SmallControlBtn(Icons.Filled.KeyboardArrowUp,   deviceColor,           { onCoverOpen?.invoke() })
                        SmallControlBtn(Icons.Filled.Stop,              HomeColors.OnSurfaceDim, { onCoverStop?.invoke() })
                        SmallControlBtn(Icons.Filled.KeyboardArrowDown, HomeColors.StateOff,    { onCoverClose?.invoke() })
                    }
                }

                isClimate && targetTemp != null && onSetTemperature != null -> {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        SmallControlBtn(Icons.Filled.Remove, deviceColor, { onSetTemperature(targetTemp - 0.5f) })
                        Text(
                            text       = "→ ${"%.1f".format(targetTemp)}°",
                            color      = HomeColors.OnSurface,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        SmallControlBtn(Icons.Filled.Add, deviceColor, { onSetTemperature(targetTemp + 0.5f) })
                    }
                }

                isLight && brightness != null && onSetBrightness != null -> {
                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Slider(
                            value                = sliderBright,
                            onValueChange        = { sliderBright = it },
                            onValueChangeFinished = { onSetBrightness(sliderBright.toInt()) },
                            valueRange           = 0f..100f,
                            colors               = SliderDefaults.colors(
                                thumbColor        = deviceColor,
                                activeTrackColor  = deviceColor,
                                inactiveTrackColor = deviceColor.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        )
                        Text("${sliderBright.toInt()}%", color = HomeColors.OnSurfaceDim, fontSize = 9.sp)
                    }
                }

                !isSensor && !isCamera -> {
                    DeviceToggle(
                        checked         = isOn,
                        enabled         = isAvailable,
                        color           = deviceColor,
                        onCheckedChange = { onToggle() }
                    )
                }
            }
        }
    }
}

// ── Small round control button ─────────────────────────────────────────────────

@Composable
private fun SmallControlBtn(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier         = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.25f), CircleShape)
            .clickable(
                indication       = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

// ── Pill toggle ────────────────────────────────────────────────────────────────

@Composable
private fun DeviceToggle(
    checked: Boolean,
    enabled: Boolean,
    color: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    val trackColor by animateColorAsState(
        targetValue   = if (checked) color.copy(alpha = 0.4f) else HomeColors.StateOff.copy(alpha = 0.4f),
        animationSpec = tween(250),
        label         = "track"
    )
    val thumbColor by animateColorAsState(
        targetValue   = if (checked) color else Color.White.copy(alpha = 0.6f),
        animationSpec = tween(250),
        label         = "thumb"
    )
    val thumbOffset by animateFloatAsState(
        targetValue   = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label         = "thumbX"
    )

    Box(
        modifier = Modifier
            .width(40.dp)
            .height(22.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(trackColor)
            .pointerInput(enabled) {
                if (enabled) detectTapGestures(onTap = { onCheckedChange(!checked) })
            }
    ) {
        val thumbDp = 16.dp
        val travel  = 18.dp
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 3.dp + travel * thumbOffset)
                .size(thumbDp)
                .clip(CircleShape)
                .background(thumbColor)
                .then(if (checked) Modifier.neonGlow(thumbColor, 6.dp, 8.dp, 0.9f) else Modifier)
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

private fun deviceColor(type: DeviceType?): Color =
    HomeColors.deviceColors[type?.name] ?: HomeColors.OnSurfaceDim

private fun iconFor(type: DeviceType?): ImageVector = when (type) {
    DeviceType.LIGHT        -> Icons.Filled.LightMode
    DeviceType.SWITCH       -> Icons.Filled.ToggleOn
    DeviceType.THERMOSTAT,
    DeviceType.CLIMATE      -> Icons.Filled.Thermostat
    DeviceType.SENSOR       -> Icons.Filled.Sensors
    DeviceType.CAMERA       -> Icons.Filled.Videocam
    DeviceType.LOCK         -> Icons.Filled.Lock
    DeviceType.COVER        -> Icons.Filled.Blinds
    DeviceType.FAN          -> Icons.Filled.Air
    DeviceType.MEDIA_PLAYER -> Icons.Filled.PlayCircle
    DeviceType.VACUUM       -> Icons.Filled.CleaningServices
    else                    -> Icons.Filled.DeviceUnknown
}

private fun SmartDevice.attrDouble(key: String): Double? {
    if (attributes == null) return null
    return try {
        val d = org.json.JSONObject(attributes).optDouble(key, Double.NaN)
        if (d.isNaN()) null else d
    } catch (e: Exception) { null }
}

private fun SmartDevice.attrInt(key: String): Int? = attrDouble(key)?.toInt()
private fun SmartDevice.attrFloat(key: String): Float? = attrDouble(key)?.toFloat()

private fun SmartDevice.attrString(key: String): String? {
    if (attributes == null) return null
    return try {
        val s = org.json.JSONObject(attributes).optString(key, "")
        if (s.isEmpty()) null else s
    } catch (e: Exception) { null }
}

private val EaseInOutQuad = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
