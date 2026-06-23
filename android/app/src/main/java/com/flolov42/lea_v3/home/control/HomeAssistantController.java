package com.flolov42.lea_v3.home.control;

import com.flolov42.lea_v3.home.models.*;
// Explicit import so 'Protocol' resolves to our enum, not okhttp3.Protocol
import com.flolov42.lea_v3.home.models.Protocol;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.*;
import okhttp3.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HomeAssistantController {

    public interface HaCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    public interface StatesCallback {
        void onStates(List<SmartDevice> devices);
        void onError(String error);
    }

    private static HomeAssistantController instance;
    private final HomeConfig config;
    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static synchronized HomeAssistantController get(HomeConfig config) {
        if (instance == null) instance = new HomeAssistantController(config);
        return instance;
    }

    private HomeAssistantController(HomeConfig config) {
        this.config = config;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
    }

    private Request.Builder authRequest(String path) {
        return new Request.Builder()
            .url(config.getHaUrl() + path)
            .header("Authorization", "Bearer " + config.getHaToken())
            .header("Content-Type", "application/json");
    }

    // ── States ────────────────────────────────────────────────────────────────

    public void fetchAllStates(StatesCallback cb) {
        Request req = authRequest("/api/states").get().build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> cb.onError("HTTP " + response.code()));
                    return;
                }
                try {
                    List<SmartDevice> devices = parseStates(body);
                    mainHandler.post(() -> cb.onStates(devices));
                } catch (Exception e) {
                    mainHandler.post(() -> cb.onError("Parse error: " + e.getMessage()));
                }
            }
        });
    }

    private List<SmartDevice> parseStates(String json) {
        List<SmartDevice> list = new ArrayList<>();
        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
        for (JsonElement el : arr) {
            try {
                JsonObject obj   = el.getAsJsonObject();
                String entityId  = obj.get("entity_id").getAsString();
                String state     = obj.get("state").getAsString();
                JsonObject attrs = obj.has("attributes") ? obj.getAsJsonObject("attributes") : new JsonObject();
                String name = attrs.has("friendly_name")
                    ? attrs.get("friendly_name").getAsString() : entityId;

                SmartDevice d = new SmartDevice();
                d.entityId     = entityId;
                d.friendlyName = name;
                d.state        = state;
                d.protocol     = Protocol.HOME_ASSISTANT;
                d.type         = entityIdToType(entityId);
                d.room         = attrs.has("area_id") ? attrs.get("area_id").getAsString() : guessRoom(name);
                d.attributes   = attrs.toString();
                list.add(d);
            } catch (Exception e) {
                // entité malformée ignorée — le reste du batch continue
            }
        }
        return list;
    }

    private DeviceType entityIdToType(String entityId) {
        String domain = entityId.contains(".") ? entityId.split("\\.")[0] : "";
        switch (domain) {
            case "light":         return DeviceType.LIGHT;
            case "switch":        return DeviceType.SWITCH;
            case "climate":       return DeviceType.CLIMATE;
            case "sensor":
            case "binary_sensor": return DeviceType.SENSOR;
            case "camera":        return DeviceType.CAMERA;
            case "lock":          return DeviceType.LOCK;
            case "cover":         return DeviceType.COVER;
            case "fan":           return DeviceType.FAN;
            case "media_player":  return DeviceType.MEDIA_PLAYER;
            case "vacuum":        return DeviceType.VACUUM;
            default:              return DeviceType.UNKNOWN;
        }
    }

    private String guessRoom(String name) {
        String n = name.toLowerCase();
        if (n.contains("salon") || n.contains("living"))  return "Salon";
        if (n.contains("chambre") || n.contains("bedroom")) return "Chambre";
        if (n.contains("cuisine") || n.contains("kitchen")) return "Cuisine";
        if (n.contains("salle de bain") || n.contains("bathroom")) return "Salle de bain";
        if (n.contains("bureau") || n.contains("office")) return "Bureau";
        if (n.contains("couloir") || n.contains("hallway")) return "Couloir";
        if (n.contains("jardin") || n.contains("garden")) return "Jardin";
        return "Général";
    }

    // ── Service calls ─────────────────────────────────────────────────────────

    public void toggle(String entityId, HaCallback cb) {
        String domain = entityId.split("\\.")[0];
        callService(domain, "toggle",
            "{\"entity_id\":\"" + entityId + "\"}", cb);
    }

    public void turnOn(String entityId, HaCallback cb) {
        String domain = entityId.split("\\.")[0];
        callService(domain, "turn_on",
            "{\"entity_id\":\"" + entityId + "\"}", cb);
    }

    public void turnOff(String entityId, HaCallback cb) {
        String domain = entityId.split("\\.")[0];
        callService(domain, "turn_off",
            "{\"entity_id\":\"" + entityId + "\"}", cb);
    }

    public void setBrightness(String entityId, int pct, HaCallback cb) {
        callService("light", "turn_on",
            "{\"entity_id\":\"" + entityId + "\",\"brightness_pct\":" + pct + "}", cb);
    }

    public void setTemperature(String entityId, float temp, HaCallback cb) {
        callService("climate", "set_temperature",
            "{\"entity_id\":\"" + entityId + "\",\"temperature\":" + temp + "}", cb);
    }

    public void activateScene(String sceneId, HaCallback cb) {
        callService("scene", "turn_on",
            "{\"entity_id\":\"" + sceneId + "\"}", cb);
    }

    public void openCover(String entityId, HaCallback cb) {
        callService("cover", "open_cover",
            "{\"entity_id\":\"" + entityId + "\"}", cb);
    }

    public void closeCover(String entityId, HaCallback cb) {
        callService("cover", "close_cover",
            "{\"entity_id\":\"" + entityId + "\"}", cb);
    }

    public void stopCover(String entityId, HaCallback cb) {
        callService("cover", "stop_cover",
            "{\"entity_id\":\"" + entityId + "\"}", cb);
    }

    private void callService(String domain, String service, String bodyJson, HaCallback cb) {
        if (!config.isConfigured()) {
            if (cb != null) cb.onError("Home Assistant non configuré");
            return;
        }
        RequestBody body = RequestBody.create(bodyJson, MediaType.parse("application/json"));
        Request req = authRequest("/api/services/" + domain + "/" + service).post(body).build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (cb != null) mainHandler.post(() -> cb.onError(e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String result = response.body() != null ? response.body().string() : "";
                if (cb != null) {
                    if (response.isSuccessful()) mainHandler.post(() -> cb.onSuccess(result));
                    else mainHandler.post(() -> cb.onError("HTTP " + response.code()));
                }
            }
        });
    }

    // ── Config check ──────────────────────────────────────────────────────────

    /** Test une connexion HA sans sauvegarder les valeurs dans la config. */
    public static void checkConnectionWith(String url, String token, HaCallback cb) {
        OkHttpClient c = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
        Handler h = new Handler(Looper.getMainLooper());
        Request req = new Request.Builder()
            .url(url + "/api/")
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .get().build();
        c.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                h.post(() -> cb.onError(e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                response.close();
                if (response.isSuccessful()) h.post(() -> cb.onSuccess("OK"));
                else h.post(() -> cb.onError("HTTP " + response.code()));
            }
        });
    }

    public void checkConnection(HaCallback cb) {
        Request req = authRequest("/api/").get().build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) mainHandler.post(() -> cb.onSuccess("OK"));
                else mainHandler.post(() -> cb.onError("HTTP " + response.code()));
            }
        });
    }
}
