package com.flolov42.lea_v3.home.models;

import android.content.Context;
import android.content.SharedPreferences;

public class HomeConfig {

    private static final String PREFS  = "lea_home_config";
    private static final String KEY_HA_URL   = "ha_url";
    private static final String KEY_HA_TOKEN = "ha_token";
    private static final String KEY_HA_ENABLED = "ha_enabled";
    private static final String KEY_WS_URL   = "ws_url";

    private final SharedPreferences prefs;

    private static HomeConfig instance;

    public static synchronized HomeConfig get(Context ctx) {
        if (instance == null) instance = new HomeConfig(ctx.getApplicationContext());
        return instance;
    }

    private HomeConfig(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String  getHaUrl()       { return prefs.getString(KEY_HA_URL, "http://homeassistant.local:8123"); }
    public String  getHaToken()     { return prefs.getString(KEY_HA_TOKEN, ""); }
    public boolean isHaEnabled()    { return prefs.getBoolean(KEY_HA_ENABLED, false); }
    public String  getWsUrl()       { return prefs.getString(KEY_WS_URL, ""); }

    public void setHaUrl(String url)       { prefs.edit().putString(KEY_HA_URL, url).apply(); }
    public void setHaToken(String token)   { prefs.edit().putString(KEY_HA_TOKEN, token).apply(); }
    public void setHaEnabled(boolean en)   { prefs.edit().putBoolean(KEY_HA_ENABLED, en).apply(); }
    public void setWsUrl(String url)       { prefs.edit().putString(KEY_WS_URL, url).apply(); }

    public boolean isConfigured() {
        return isHaEnabled() && !getHaToken().isEmpty();
    }
}
