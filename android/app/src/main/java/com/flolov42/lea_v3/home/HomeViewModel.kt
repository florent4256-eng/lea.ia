package com.flolov42.lea_v3.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flolov42.lea_v3.home.control.DeviceControlManager
import com.flolov42.lea_v3.home.database.LeaHomeDatabase
import com.flolov42.lea_v3.home.discovery.DeviceDiscoveryService
import com.flolov42.lea_v3.home.models.SmartDevice
import com.flolov42.lea_v3.home.network.LeaHomeWebSocketListener
import com.flolov42.lea_v3.home.ui.ChatMessage
import com.flolov42.lea_v3.home.ui.HomeUiState
import com.flolov42.lea_v3.home.voice.LeaHomeVoiceProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _devices          = MutableStateFlow<List<SmartDevice>>(emptyList())
    val devices: StateFlow<List<SmartDevice>> = _devices.asStateFlow()

    private val _rooms            = MutableStateFlow<List<String>>(emptyList())
    val rooms: StateFlow<List<String>> = _rooms.asStateFlow()

    private val _selectedRoom     = MutableStateFlow<String?>(null)
    val selectedRoom: StateFlow<String?> = _selectedRoom.asStateFlow()

    private val _uiState          = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _messages         = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isListening      = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _wsConnected      = MutableStateFlow(false)
    val wsConnected: StateFlow<Boolean> = _wsConnected.asStateFlow()

    private val _scanFoundDevices = MutableStateFlow<List<SmartDevice>>(emptyList())
    val scanFoundDevices: StateFlow<List<SmartDevice>> = _scanFoundDevices.asStateFlow()

    private val _showFavorites    = MutableStateFlow(false)
    val showFavorites: StateFlow<Boolean> = _showFavorites.asStateFlow()

    private val _scenes           = MutableStateFlow<List<LeaHomeDatabase.Scene>>(emptyList())
    val scenes: StateFlow<List<LeaHomeDatabase.Scene>> = _scenes.asStateFlow()

    // Guard: true once services are wired, survives rotation
    var servicesReady = false

    lateinit var db: LeaHomeDatabase
    lateinit var control: DeviceControlManager
    lateinit var discovery: DeviceDiscoveryService
    lateinit var wsListener: LeaHomeWebSocketListener
    lateinit var voiceProcessor: LeaHomeVoiceProcessor

    fun init() {
        if (_messages.value.isEmpty()) {
            addLeaMessage("Bonjour ! Je suis Léa Home. Dites-moi ce que vous voulez contrôler.")
        }
        refreshDevices()
        if (!_wsConnected.value) setupWebSocket()
    }

    // ── Device refresh (IO thread) ────────────────────────────────────────────

    fun loadFromDb() { refreshDevices() }

    fun refreshDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            loadDevicesInternal()
        }
    }

    private fun loadDevicesInternal() {
        val room = _selectedRoom.value
        _devices.value = when {
            _showFavorites.value -> db.getFavorites()
            room != null         -> db.getDevicesByRoom(room)
            else                 -> db.getAllDevices()
        }
        _rooms.value  = db.getRooms()
        _scenes.value = db.getScenes(null)
    }

    // ── Room / Favorites filters ──────────────────────────────────────────────

    fun selectRoom(room: String?) {
        _selectedRoom.value = room
        if (room != null) _showFavorites.value = false
        refreshDevices()
    }

    fun toggleFavoriteFilter() {
        _showFavorites.value = !_showFavorites.value
        if (_showFavorites.value) _selectedRoom.value = null
        refreshDevices()
    }

    fun toggleFavoriteDevice(device: SmartDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            db.setFavorite(device.entityId, !device.isFavorite)
            loadDevicesInternal()
        }
    }

    // ── Device CRUD ───────────────────────────────────────────────────────────

    fun addDevice(device: SmartDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            db.upsertDevice(device)
            loadDevicesInternal()
        }
        addLeaMessage("« ${device.friendlyName} » ajouté !")
    }

    fun deleteDevice(entityId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.deleteDevice(entityId)
            loadDevicesInternal()
        }
    }

    // ── Device control ────────────────────────────────────────────────────────

    fun toggleDevice(device: SmartDevice) {
        control.toggle(device, object : DeviceControlManager.ControlCallback {
            override fun onSuccess(dev: SmartDevice, state: String) {
                dev.state = state
                refreshDevices()
            }
            override fun onError(dev: SmartDevice, err: String) {
                _uiState.value = HomeUiState.Error(err)
            }
        })
    }

    fun setBrightness(device: SmartDevice, pct: Int) {
        control.setBrightness(device, pct, object : DeviceControlManager.ControlCallback {
            override fun onSuccess(dev: SmartDevice, state: String) { refreshDevices() }
            override fun onError(dev: SmartDevice, err: String) { _uiState.value = HomeUiState.Error(err) }
        })
    }

    fun setTemperature(device: SmartDevice, temp: Float) {
        control.setTemperature(device, temp, object : DeviceControlManager.ControlCallback {
            override fun onSuccess(dev: SmartDevice, state: String) { refreshDevices() }
            override fun onError(dev: SmartDevice, err: String) { _uiState.value = HomeUiState.Error(err) }
        })
    }

    fun openCover(device: SmartDevice) {
        control.openCover(device, object : DeviceControlManager.ControlCallback {
            override fun onSuccess(dev: SmartDevice, state: String) { refreshDevices() }
            override fun onError(dev: SmartDevice, err: String) { _uiState.value = HomeUiState.Error(err) }
        })
    }

    fun closeCover(device: SmartDevice) {
        control.closeCover(device, object : DeviceControlManager.ControlCallback {
            override fun onSuccess(dev: SmartDevice, state: String) { refreshDevices() }
            override fun onError(dev: SmartDevice, err: String) { _uiState.value = HomeUiState.Error(err) }
        })
    }

    fun stopCover(device: SmartDevice) {
        control.stopCover(device, object : DeviceControlManager.ControlCallback {
            override fun onSuccess(dev: SmartDevice, state: String) { refreshDevices() }
            override fun onError(dev: SmartDevice, err: String) { _uiState.value = HomeUiState.Error(err) }
        })
    }

    // ── Scenes ────────────────────────────────────────────────────────────────

    fun createScene(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val onDevices = _devices.value.filter { it.isOn() }
            if (onDevices.isEmpty()) {
                addLeaMessage("Aucun appareil allumé à sauvegarder dans la scène")
                return@launch
            }
            val cmds = try {
                val arr = org.json.JSONArray()
                onDevices.forEach { d ->
                    val obj = org.json.JSONObject()
                    obj.put("entityId", d.entityId)
                    obj.put("state", d.state)
                    arr.put(obj)
                }
                arr.toString()
            } catch (e: Exception) { "[]" }

            db.saveScene(name.trim(), null, "✨", cmds)
            loadDevicesInternal()
            addLeaMessage("Scène « ${name.trim()} » créée avec ${onDevices.size} appareil(s) !")
        }
    }

    fun activateLocalScene(scene: LeaHomeDatabase.Scene) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val arr = org.json.JSONArray(scene.commands ?: "[]")
                val allDevices = db.getAllDevices()
                var count = 0
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val entityId = obj.getString("entityId")
                    val device = allDevices.find { it.entityId == entityId }
                    if (device != null) {
                        count++
                        control.turnOn(device, null)
                    }
                }
                addLeaMessage("Scène « ${scene.name} » : $count appareil(s) activé(s)")
                loadDevicesInternal()
            } catch (e: Exception) {
                addLeaMessage("Erreur scène : ${e.message}")
            }
        }
    }

    fun deleteScene(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.deleteScene(id)
            loadDevicesInternal()
        }
    }

    // ── Network scan ──────────────────────────────────────────────────────────

    fun startNetworkScan() {
        _uiState.value = HomeUiState.Discovering
        _scanFoundDevices.value = emptyList()
        discovery.discoverWifiOnly(object : DeviceDiscoveryService.DiscoveryListener {
            override fun onDeviceFound(d: SmartDevice) {
                _scanFoundDevices.value = _scanFoundDevices.value + d
                refreshDevices()
            }
            override fun onComplete(all: List<SmartDevice>) {
                _uiState.value = HomeUiState.Idle
                refreshDevices()
            }
            override fun onError(error: String) {
                _uiState.value = HomeUiState.Idle
            }
        })
    }

    fun startDiscovery() {
        _uiState.value = HomeUiState.Discovering
        _scanFoundDevices.value = emptyList()
        discovery.discoverAll(object : DeviceDiscoveryService.DiscoveryListener {
            override fun onDeviceFound(d: SmartDevice) {
                _scanFoundDevices.value = _scanFoundDevices.value + d
                refreshDevices()
            }
            override fun onComplete(all: List<SmartDevice>) {
                _uiState.value = HomeUiState.Idle
                refreshDevices()
                if (all.isEmpty()) {
                    addLeaMessage("Aucun appareil détecté. Vérifiez votre réseau WiFi.")
                } else {
                    addLeaMessage("${all.size} appareil(s) trouvé(s) !")
                }
            }
            override fun onError(error: String) {
                _uiState.value = HomeUiState.Error(error)
                addLeaMessage("Erreur de découverte : $error")
            }
        })
    }

    // ── Voice ─────────────────────────────────────────────────────────────────

    fun processVoiceCommand(command: String) {
        addUserMessage(command)
        voiceProcessor.process(command, object : LeaHomeVoiceProcessor.VoiceResponse {
            override fun onSpeak(text: String)  { addLeaMessage(text) }
            override fun onAction(desc: String) { addLeaMessage(desc); refreshDevices() }
        })
    }

    fun setListening(l: Boolean) { _isListening.value = l }

    // ── WebSocket ─────────────────────────────────────────────────────────────

    private fun setupWebSocket() {
        wsListener.setListener(object : LeaHomeWebSocketListener.StateChangeListener {
            override fun onDeviceStateChanged(d: SmartDevice) { refreshDevices() }
            override fun onConnected()    { _wsConnected.value = true }
            override fun onDisconnected() { _wsConnected.value = false }
        })
        Thread { wsListener.connect() }.start()
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    fun addLeaMessage(text: String) {
        _messages.value = _messages.value + ChatMessage(text = text, isLea = true)
    }

    private fun addUserMessage(text: String) {
        _messages.value = _messages.value + ChatMessage(text = text, isLea = false)
    }

    override fun onCleared() {
        super.onCleared()
        if (servicesReady) {
            wsListener.disconnect()
            discovery.stop()
        }
    }
}
