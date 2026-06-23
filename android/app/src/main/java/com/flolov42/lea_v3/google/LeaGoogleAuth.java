package com.flolov42.lea_v3.google;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;

import java.util.Arrays;

public class LeaGoogleAuth {

    public static final int RC_SIGN_IN = 9001;

    // OAuth scopes demandés
    public static final String SCOPE_CALENDAR  = "https://www.googleapis.com/auth/calendar";
    public static final String SCOPE_TASKS      = "https://www.googleapis.com/auth/tasks";
    public static final String SCOPE_CONTACTS   = "https://www.googleapis.com/auth/contacts.readonly";
    public static final String SCOPE_GMAIL_READ = "https://www.googleapis.com/auth/gmail.readonly";
    public static final String SCOPE_GMAIL_SEND = "https://www.googleapis.com/auth/gmail.send";
    public static final String SCOPE_DRIVE      = "https://www.googleapis.com/auth/drive.file";

    private static final String PREFS = "lea_google_auth";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME  = "name";
    private static final String KEY_PHOTO = "photo";

    public interface SignInCallback { void onSuccess(String email, String name); void onError(String error); }
    public interface TokenCallback  { void onToken(String token); void onError(String error); }

    private static LeaGoogleAuth instance;
    public static LeaGoogleAuth get(Context ctx) {
        if (instance == null) instance = new LeaGoogleAuth(ctx.getApplicationContext());
        return instance;
    }

    private final Context ctx;
    private final LeaGoogleDatabase db;

    private LeaGoogleAuth(Context ctx) {
        this.ctx = ctx;
        this.db  = LeaGoogleDatabase.get(ctx);
    }

    // ── Sign-In ───────────────────────────────────────────────────────────────
    public void signIn(Activity activity) {
        GoogleSignInOptions gso = buildGso();
        GoogleSignInClient client = GoogleSignIn.getClient(activity, gso);
        activity.startActivityForResult(client.getSignInIntent(), RC_SIGN_IN);
    }

    public void handleSignInResult(Intent data, SignInCallback callback) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            String email   = account.getEmail()       != null ? account.getEmail()       : "";
            String name    = account.getDisplayName()  != null ? account.getDisplayName()  : "";
            String photo   = account.getPhotoUrl()     != null ? account.getPhotoUrl().toString() : "";
            // Persist
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_EMAIL, email).putString(KEY_NAME, name).putString(KEY_PHOTO, photo).apply();
            db.saveAuth(email, name, photo);
            callback.onSuccess(email, name);
        } catch (ApiException e) {
            callback.onError("Erreur Google Sign-In: " + e.getStatusCode());
        }
    }

    // ── Token retrieval (appeler sur thread background) ───────────────────────
    public void getAccessToken(TokenCallback callback) {
        new Thread(() -> {
            try {
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(ctx);
                if (account == null) { callback.onError("Non connecté"); return; }
                String scopes = "oauth2:" + SCOPE_CALENDAR + " " + SCOPE_TASKS + " " + SCOPE_CONTACTS
                              + " " + SCOPE_GMAIL_READ + " " + SCOPE_GMAIL_SEND + " " + SCOPE_DRIVE;
                String token = GoogleAuthUtil.getToken(ctx, account.getAccount(), scopes);
                db.saveToken(token, System.currentTimeMillis() + 3500_000L);
                callback.onToken(token);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // ── State ─────────────────────────────────────────────────────────────────
    public boolean isSignedIn() {
        return GoogleSignIn.getLastSignedInAccount(ctx) != null;
    }

    public String getEmail() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(ctx);
        if (account != null && account.getEmail() != null) return account.getEmail();
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_EMAIL, "");
    }

    public String getDisplayName() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(ctx);
        if (account != null && account.getDisplayName() != null) return account.getDisplayName();
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_NAME, "");
    }

    // ── Sign-Out ──────────────────────────────────────────────────────────────
    public void signOut(Activity activity, Runnable onComplete) {
        GoogleSignIn.getClient(activity, buildGso()).signOut().addOnCompleteListener(t -> {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
            db.clearAuth();
            if (onComplete != null) onComplete.run();
        });
    }

    // ── Revoke ────────────────────────────────────────────────────────────────
    public void revokeAccess(Activity activity, Runnable onComplete) {
        GoogleSignIn.getClient(activity, buildGso()).revokeAccess().addOnCompleteListener(t -> {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
            db.clearAuth();
            if (onComplete != null) onComplete.run();
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private GoogleSignInOptions buildGso() {
        return new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                new Scope(SCOPE_CALENDAR),
                new Scope(SCOPE_TASKS),
                new Scope(SCOPE_CONTACTS),
                new Scope(SCOPE_GMAIL_READ),
                new Scope(SCOPE_GMAIL_SEND),
                new Scope(SCOPE_DRIVE)
            )
            .build();
    }
}
