package com.flolov42.lea_v3.modes;

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

public abstract class LeaBaseMode {

    protected final Context             ctx;
    protected final String              modeId;
    protected final LeaModeDatabase     db;
    protected final LeaModeNotifications notif;

    public LeaBaseMode(Context ctx, String modeId) {
        this.ctx    = ctx;
        this.modeId = modeId;
        this.db     = LeaModeDatabase.get(ctx);
        this.notif  = LeaModeNotifications.get(ctx);
    }

    public abstract void execute();

    protected void log(String msg) { db.log(modeId, msg); }

    protected void notify(String title, String msg) { notif.notify(modeId, title, msg); }
}
