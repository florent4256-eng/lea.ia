package com.flolov42.lea_v3.home.discovery;

import com.flolov42.lea_v3.home.control.HomeAssistantController;
import com.flolov42.lea_v3.home.database.LeaHomeDatabase;
import com.flolov42.lea_v3.home.models.*;

import android.content.Context;
import java.util.List;

public class HomeAssistantDiscovery {

    public interface DiscoveryCallback {
        void onDiscovered(List<SmartDevice> devices);
        void onError(String error);
    }

    private final Context ctx;
    private final HomeAssistantController ha;
    private final LeaHomeDatabase db;

    public HomeAssistantDiscovery(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.ha  = HomeAssistantController.get(HomeConfig.get(ctx));
        this.db  = LeaHomeDatabase.get(ctx);
    }

    public void discover(DiscoveryCallback cb) {
        if (!HomeConfig.get(ctx).isConfigured()) {
            if (cb != null) cb.onError("Home Assistant non configuré — ouvrez les paramètres");
            return;
        }
        ha.fetchAllStates(new HomeAssistantController.StatesCallback() {
            @Override public void onStates(List<SmartDevice> devices) {
                for (SmartDevice d : devices) {
                    if (d.type != DeviceType.UNKNOWN) db.upsertDevice(d);
                }
                if (cb != null) cb.onDiscovered(devices);
            }
            @Override public void onError(String error) {
                if (cb != null) cb.onError(error);
            }
        });
    }
}
