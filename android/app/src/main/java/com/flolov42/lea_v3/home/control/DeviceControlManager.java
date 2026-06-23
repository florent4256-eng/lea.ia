package com.flolov42.lea_v3.home.control;

import com.flolov42.lea_v3.home.database.LeaHomeDatabase;
import com.flolov42.lea_v3.home.models.*;

import android.content.Context;

public class DeviceControlManager {

    public interface ControlCallback {
        void onSuccess(SmartDevice device, String newState);
        void onError(SmartDevice device, String error);
    }

    private static DeviceControlManager instance;
    private final HomeAssistantController ha;
    private final WifiDirectController wifi;
    private final LeaHomeDatabase db;

    public static synchronized DeviceControlManager get(Context ctx) {
        if (instance == null) instance = new DeviceControlManager(ctx.getApplicationContext());
        return instance;
    }

    private DeviceControlManager(Context ctx) {
        ha   = HomeAssistantController.get(HomeConfig.get(ctx));
        wifi = WifiDirectController.get();
        db   = LeaHomeDatabase.get(ctx);
    }

    public void toggle(SmartDevice device, ControlCallback cb) {
        String newState = device.isOn() ? "off" : "on";
        if (device.protocol == Protocol.HOME_ASSISTANT) {
            ha.toggle(device.entityId, new HomeAssistantController.HaCallback() {
                @Override public void onSuccess(String r) {
                    db.updateDeviceState(device.entityId, newState, null);
                    db.log(device.entityId, "toggle → " + newState, "ok");
                    if (cb != null) cb.onSuccess(device, newState);
                }
                @Override public void onError(String e) {
                    db.log(device.entityId, "toggle", "error: " + e);
                    if (cb != null) cb.onError(device, e);
                }
            });
        } else {
            wifi.toggle(device, new WifiDirectController.WifiCallback() {
                @Override public void onSuccess(String r) {
                    db.updateDeviceState(device.entityId, newState, null);
                    db.log(device.entityId, "toggle → " + newState, "ok");
                    if (cb != null) cb.onSuccess(device, newState);
                }
                @Override public void onError(String e) {
                    db.log(device.entityId, "toggle", "error: " + e);
                    if (cb != null) cb.onError(device, e);
                }
            });
        }
    }

    public void turnOn(SmartDevice device, ControlCallback cb) {
        if (device.protocol == Protocol.HOME_ASSISTANT) {
            ha.turnOn(device.entityId, resultCallback(device, "on", "turn_on", cb));
        } else {
            wifi.turnOn(device, wifiResultCallback(device, "on", cb));
        }
    }

    public void turnOff(SmartDevice device, ControlCallback cb) {
        if (device.protocol == Protocol.HOME_ASSISTANT) {
            ha.turnOff(device.entityId, resultCallback(device, "off", "turn_off", cb));
        } else {
            wifi.turnOff(device, wifiResultCallback(device, "off", cb));
        }
    }

    public void setBrightness(SmartDevice device, int pct, ControlCallback cb) {
        if (device.protocol == Protocol.HOME_ASSISTANT) {
            ha.setBrightness(device.entityId, pct, resultCallback(device, "on", "brightness=" + pct, cb));
        } else {
            wifi.setBrightness(device, pct, wifiResultCallback(device, "on", cb));
        }
    }

    public void setTemperature(SmartDevice device, float temp, ControlCallback cb) {
        ha.setTemperature(device.entityId, temp, resultCallback(device, String.valueOf(temp), "set_temp", cb));
    }

    public void openCover(SmartDevice device, ControlCallback cb) {
        ha.openCover(device.entityId, resultCallback(device, "open", "open_cover", cb));
    }

    public void closeCover(SmartDevice device, ControlCallback cb) {
        ha.closeCover(device.entityId, resultCallback(device, "closed", "close_cover", cb));
    }

    public void stopCover(SmartDevice device, ControlCallback cb) {
        ha.stopCover(device.entityId, resultCallback(device, device.state, "stop_cover", cb));
    }

    private HomeAssistantController.HaCallback resultCallback(
            SmartDevice device, String newState, String action, ControlCallback cb) {
        return new HomeAssistantController.HaCallback() {
            @Override public void onSuccess(String r) {
                db.updateDeviceState(device.entityId, newState, null);
                db.log(device.entityId, action, "ok");
                if (cb != null) cb.onSuccess(device, newState);
            }
            @Override public void onError(String e) {
                db.log(device.entityId, action, "error: " + e);
                if (cb != null) cb.onError(device, e);
            }
        };
    }

    private WifiDirectController.WifiCallback wifiResultCallback(
            SmartDevice device, String newState, ControlCallback cb) {
        return new WifiDirectController.WifiCallback() {
            @Override public void onSuccess(String r) {
                db.updateDeviceState(device.entityId, newState, null);
                if (cb != null) cb.onSuccess(device, newState);
            }
            @Override public void onError(String e) {
                if (cb != null) cb.onError(device, e);
            }
        };
    }
}
