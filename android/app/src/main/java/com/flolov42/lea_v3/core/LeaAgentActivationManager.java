package com.flolov42.lea_v3.core;

import com.flolov42.lea_v3.ui.*;
import com.flolov42.lea_v3.agents.*;
import com.flolov42.lea_v3.modes.*;
import com.flolov42.lea_v3.plus.gamification.*;
import com.flolov42.lea_v3.plus.lifestyle.*;
import com.flolov42.lea_v3.plus.learning.*;
import com.flolov42.lea_v3.plus.premium.*;
import com.flolov42.lea_v3.plus.connect.*;
import com.flolov42.lea_v3.bixby.*;
import com.flolov42.lea_v3.routines.*;
import com.flolov42.lea_v3.telephony.*;
import com.flolov42.lea_v3.code.*;
import com.flolov42.lea_v3.core.*;
import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.notifications.*;
import com.flolov42.lea_v3.utilities.*;


import android.content.Context;
import android.content.SharedPreferences;

public class LeaAgentActivationManager {

    // Agent IDs — used as keys everywhere
    public static final String EMAIL        = "EMAIL";
    public static final String NOTIFICATION = "NOTIFICATION";
    public static final String CALENDAR     = "CALENDAR";
    public static final String FINANCE      = "FINANCE";
    public static final String HEALTH       = "HEALTH";
    public static final String PRODUCTIVITY = "PRODUCTIVITY";
    public static final String SOCIAL       = "SOCIAL";
    public static final String SMART_HOME   = "SMART_HOME";
    public static final String LEARNING     = "LEARNING";
    public static final String SECURITY     = "SECURITY";
    public static final String CODE         = "CODE";

    private static final String PREFS = "lea_agent_prefs";

    private final Context             ctx;
    private final LeaAgentDatabase    db;
    private final SharedPreferences   prefs;

    private static LeaAgentActivationManager instance;

    public static synchronized LeaAgentActivationManager get(Context ctx) {
        if (instance == null) instance = new LeaAgentActivationManager(ctx.getApplicationContext());
        return instance;
    }

    private LeaAgentActivationManager(Context ctx) {
        this.ctx   = ctx;
        this.db    = LeaAgentDatabase.get(ctx);
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isEnabled(String agentId) {
        return db.isEnabled(agentId);
    }

    public void setEnabled(String agentId, boolean enabled) {
        db.setEnabled(agentId, enabled);
        // Notify service to start/stop agent
        LeaAgentService svc = LeaAgentService.instance;
        if (svc != null) {
            if (enabled) svc.startAgent(agentId);
            else         svc.stopAgent(agentId);
        }
    }

    public void logAction(String agentId, String message) {
        db.addLog(agentId, message);
        db.updateLastAction(agentId, message);
    }

    public java.util.List<LeaAgentDatabase.LogRow> getLogs(String agentId) {
        return db.getLogs(agentId, 30);
    }
}
