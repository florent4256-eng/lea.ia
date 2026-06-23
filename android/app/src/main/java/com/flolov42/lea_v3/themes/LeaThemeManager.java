package com.flolov42.lea_v3.themes;

import com.flolov42.lea_v3.database.*;

import android.content.Context;

public class LeaThemeManager {

    public static final String GALAXIE = "galaxie";
    public static final String MINIMAL  = "minimal";
    public static final String OCEAN    = "ocean";
    public static final String FOREST   = "forest";
    public static final String SUNSET   = "sunset";
    public static final String NEON     = "neon";

    private static LeaThemeManager instance;
    private final Context ctx;

    public static synchronized LeaThemeManager get(Context ctx) {
        if (instance == null) instance = new LeaThemeManager(ctx.getApplicationContext());
        return instance;
    }
    private LeaThemeManager(Context ctx) { this.ctx = ctx; }

    public static class ThemeColors {
        public int bg, primary, card, text, accent;
        public String name, emoji;
    }

    public ThemeColors getColors() { return getColors(getCurrentTheme()); }

    public ThemeColors getColors(String theme) {
        ThemeColors c = new ThemeColors();
        switch (theme) {
            case MINIMAL:
                c.bg=0xFF0A0A0A; c.primary=0xFFE0E0E0; c.card=0xFF1A1A1A;
                c.text=0xFFFFFFFF; c.accent=0xFF9E9E9E; c.name="Minimal"; c.emoji="⬛"; break;
            case OCEAN:
                c.bg=0xFF001F3F; c.primary=0xFF00B4D8; c.card=0xFF023E8A;
                c.text=0xFFFFFFFF; c.accent=0xFF90E0EF; c.name="Ocean"; c.emoji="🌊"; break;
            case FOREST:
                c.bg=0xFF0A1F0A; c.primary=0xFF4CAF50; c.card=0xFF1B3A1B;
                c.text=0xFFFFFFFF; c.accent=0xFF81C784; c.name="Forêt"; c.emoji="🌿"; break;
            case SUNSET:
                c.bg=0xFF1A0A00; c.primary=0xFFFF6B35; c.card=0xFF2D1200;
                c.text=0xFFFFFFFF; c.accent=0xFFFFD166; c.name="Coucher de Soleil"; c.emoji="🌅"; break;
            case NEON:
                c.bg=0xFF000000; c.primary=0xFFFF00FF; c.card=0xFF0D0D0D;
                c.text=0xFFFFFFFF; c.accent=0xFF00FF41; c.name="Neon"; c.emoji="⚡"; break;
            default: // GALAXIE
                c.bg=0xFF011627; c.primary=0xFF00E5FF; c.card=0xFF012040;
                c.text=0xFFFFFFFF; c.accent=0xFF00B0CC; c.name="Galaxie"; c.emoji="🌌"; break;
        }
        return c;
    }

    public String getCurrentTheme() {
        LeaFeaturesDatabase.ThemeCfg cfg = LeaFeaturesDatabase.get(ctx).getThemeConfig();
        return cfg.currentTheme != null ? cfg.currentTheme : GALAXIE;
    }

    public void applyTheme(String theme) {
        LeaFeaturesDatabase db = LeaFeaturesDatabase.get(ctx);
        LeaFeaturesDatabase.ThemeCfg cfg = db.getThemeConfig();
        db.saveThemeConfig(theme, cfg.autoSwitch, cfg.eyeCare, cfg.customColors);
        if (!theme.equals(GALAXIE)) {
            com.flolov42.lea_v3.achievements.LeaAchievementManager.get(ctx).trigger("DARK_MODE");
        }
    }

    public boolean isEyeCareEnabled() {
        return LeaFeaturesDatabase.get(ctx).getThemeConfig().eyeCare == 1;
    }

    public void setEyeCare(boolean enabled) {
        LeaFeaturesDatabase db = LeaFeaturesDatabase.get(ctx);
        LeaFeaturesDatabase.ThemeCfg cfg = db.getThemeConfig();
        db.saveThemeConfig(cfg.currentTheme, cfg.autoSwitch, enabled ? 1 : 0, cfg.customColors);
    }

    public static String[] allThemes() {
        return new String[]{GALAXIE, MINIMAL, OCEAN, FOREST, SUNSET, NEON};
    }
}
