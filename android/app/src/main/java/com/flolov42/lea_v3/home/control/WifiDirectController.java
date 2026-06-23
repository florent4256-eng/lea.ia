package com.flolov42.lea_v3.home.control;

import com.flolov42.lea_v3.home.models.*;

import android.os.Handler;
import android.os.Looper;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class WifiDirectController {

    public interface WifiCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    private static WifiDirectController instance;
    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static synchronized WifiDirectController get() {
        if (instance == null) instance = new WifiDirectController();
        return instance;
    }

    private WifiDirectController() {
        client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();
    }

    public void sendCommand(SmartDevice device, String action, String params, WifiCallback cb) {
        if (device.ipAddress == null || device.ipAddress.isEmpty()) {
            if (cb != null) cb.onError("Adresse IP inconnue pour " + device.friendlyName);
            return;
        }
        String url = "http://" + device.ipAddress + "/cmd?action=" + action;
        if (params != null && !params.isEmpty()) url += "&" + params;

        Request req = new Request.Builder().url(url).get().build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (cb != null) mainHandler.post(() -> cb.onError(e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "OK";
                if (cb != null) mainHandler.post(() -> cb.onSuccess(body));
            }
        });
    }

    public void toggle(SmartDevice device, WifiCallback cb) {
        sendCommand(device, "toggle", null, cb);
    }

    public void turnOn(SmartDevice device, WifiCallback cb) {
        sendCommand(device, "on", null, cb);
    }

    public void turnOff(SmartDevice device, WifiCallback cb) {
        sendCommand(device, "off", null, cb);
    }

    public void setBrightness(SmartDevice device, int pct, WifiCallback cb) {
        sendCommand(device, "brightness", "value=" + pct, cb);
    }
}
