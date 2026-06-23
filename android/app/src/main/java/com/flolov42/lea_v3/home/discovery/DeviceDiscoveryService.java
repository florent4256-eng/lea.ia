package com.flolov42.lea_v3.home.discovery;

import com.flolov42.lea_v3.home.models.SmartDevice;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class DeviceDiscoveryService {

    public interface DiscoveryListener {
        void onDeviceFound(SmartDevice device);
        void onComplete(List<SmartDevice> allDevices);
        void onError(String error);
    }

    private final Context ctx;
    private final HomeAssistantDiscovery haDiscovery;
    private final WifiDiscoveryHelper wifiDiscovery;

    private final List<SmartDevice> allDevices = new ArrayList<>();

    public DeviceDiscoveryService(Context ctx) {
        this.ctx          = ctx.getApplicationContext();
        this.haDiscovery  = new HomeAssistantDiscovery(ctx);
        this.wifiDiscovery = new WifiDiscoveryHelper(ctx);
    }

    public void discoverAll(DiscoveryListener listener) {
        allDevices.clear();
        final int[] pending = {2};  // HA + WiFi

        haDiscovery.discover(new HomeAssistantDiscovery.DiscoveryCallback() {
            @Override public void onDiscovered(List<SmartDevice> devices) {
                synchronized (allDevices) { allDevices.addAll(devices); }
                for (SmartDevice d : devices) {
                    if (listener != null) listener.onDeviceFound(d);
                }
                if (--pending[0] == 0 && listener != null)
                    listener.onComplete(new ArrayList<>(allDevices));
            }
            @Override public void onError(String error) {
                if (listener != null) listener.onError("HA: " + error);
                if (--pending[0] == 0 && listener != null)
                    listener.onComplete(new ArrayList<>(allDevices));
            }
        });

        wifiDiscovery.startDiscovery(new WifiDiscoveryHelper.WifiDiscoveryCallback() {
            @Override public void onFound(SmartDevice d) {
                synchronized (allDevices) { allDevices.add(d); }
                if (listener != null) listener.onDeviceFound(d);
            }
            @Override public void onFinished(List<SmartDevice> devices) {
                if (--pending[0] == 0 && listener != null)
                    listener.onComplete(new ArrayList<>(allDevices));
            }
        });
    }

    public void discoverWifiOnly(DiscoveryListener listener) {
        allDevices.clear();
        wifiDiscovery.startDiscovery(new WifiDiscoveryHelper.WifiDiscoveryCallback() {
            @Override public void onFound(SmartDevice d) {
                synchronized (allDevices) { allDevices.add(d); }
                if (listener != null) listener.onDeviceFound(d);
            }
            @Override public void onFinished(List<SmartDevice> devices) {
                if (listener != null) listener.onComplete(new ArrayList<>(allDevices));
            }
        });
    }

    public void stop() {
        wifiDiscovery.stopDiscovery();
    }
}
