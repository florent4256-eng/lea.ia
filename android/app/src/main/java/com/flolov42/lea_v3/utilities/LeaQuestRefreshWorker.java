package com.flolov42.lea_v3.utilities;

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
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Exécution quotidienne à 6h — génère les nouvelles quêtes du jour et notifie l'utilisateur.
 */
public class LeaQuestRefreshWorker extends Worker {

    public LeaQuestRefreshWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            if (!LeaPlusManager.get(getApplicationContext()).isEnabled(LeaPlusDatabase.QUESTS)) {
                return Result.success();
            }
            LeaQuestSystemManager.get(getApplicationContext()).generateDailyQuests();
            return Result.success();
        } catch (Exception e) {
            LeaPlusDatabase.get(getApplicationContext())
                .log(LeaPlusDatabase.QUESTS, "⚠️ Worker erreur: " + e.getMessage());
            return Result.retry();
        }
    }
}
