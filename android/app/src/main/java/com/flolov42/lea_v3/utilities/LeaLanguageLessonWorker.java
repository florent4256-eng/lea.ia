package com.flolov42.lea_v3.utilities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.flolov42.lea_v3.core.LeaAgentActivationManager;
import com.flolov42.lea_v3.database.LeaPlusDatabase;
import com.flolov42.lea_v3.plus.learning.LeaLanguageContent;

public class LeaLanguageLessonWorker extends Worker {

    private static final String CHANNEL_ID = "lea_language";

    public LeaLanguageLessonWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context ctx = getApplicationContext();

            // Ne rien faire si l'agent Apprentissage est désactivé
            if (!LeaAgentActivationManager.get(ctx).isEnabled(LeaAgentActivationManager.LEARNING)) {
                return Result.success();
            }

            // Créer le canal si nécessaire
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Leçons de langue", NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(ch);

            // Lire la langue active depuis les préférences
            SharedPreferences prefs = ctx.getSharedPreferences("lea_language_prefs", Context.MODE_PRIVATE);
            String activeLang = prefs.getString("active_language", "EN");

            // S'assurer que le vocabulaire est initialisé
            LeaLanguageContent lc = LeaLanguageContent.get(ctx);
            lc.seedIfEmpty(activeLang);

            // Générer une question quiz
            LeaLanguageContent.QuizQuestion quiz = lc.generateQuiz(activeLang);
            if (quiz == null) return Result.success();

            // Notification avec le mot du jour
            NotificationCompat.Builder notif = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle("📚 Mot du jour — " + activeLang)
                .setContentText(quiz.word + " = " + quiz.correctAnswer)
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText("🔤 " + quiz.word + "\n" +
                             "📖 " + quiz.phonetic + "\n" +
                             "✏️ " + quiz.correctAnswer + "\n\n" +
                             "💬 " + quiz.example))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

            nm.notify(8200, notif.build()); // 8000 réservé à LeaAgentService foreground
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
