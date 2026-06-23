package com.flolov42.lea_v3.home.models;

public class SmartDevice {

    public long       id;
    public String     entityId;       // Home Assistant entity_id (e.g. "light.salon")
    public String     friendlyName;   // Displayed name
    public DeviceType type;
    public Protocol   protocol;
    public String     room;
    public String     state;          // "on", "off", "unavailable", numeric for sensors
    public String     attributes;     // JSON blob (brightness, temperature, …)
    public String     ipAddress;      // for direct WiFi devices
    public boolean    isFavorite;
    public long       lastSeen;

    public SmartDevice() {}

    public SmartDevice(String entityId, String friendlyName, DeviceType type, Protocol protocol, String room) {
        this.entityId     = entityId;
        this.friendlyName = friendlyName;
        this.type         = type;
        this.protocol     = protocol;
        this.room         = room;
        this.state        = "unknown";
        this.lastSeen     = System.currentTimeMillis();
    }

    public boolean isOn() {
        return "on".equalsIgnoreCase(state);
    }

    public boolean isAvailable() {
        return !"unavailable".equalsIgnoreCase(state);
    }
}
