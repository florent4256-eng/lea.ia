package com.flolov42.lea_v3.code;

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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class LeaCodeFileManager {

    private final Context ctx;
    private final File    projectsRoot;

    public LeaCodeFileManager(Context ctx) {
        this.ctx          = ctx;
        this.projectsRoot = new File(ctx.getFilesDir(), "projects");
        if (!projectsRoot.exists()) projectsRoot.mkdirs();
    }

    public File getProjectRoot(String projectName) {
        return new File(projectsRoot, sanitize(projectName));
    }

    /** Create full Android project skeleton */
    public File createAndroidProject(String projectName, String packageName) throws IOException {
        String safe      = sanitize(projectName);
        String pkgPath   = packageName.replace(".", "/");
        File   root      = new File(projectsRoot, safe);

        // Standard Android folder structure
        File javaDir = new File(root, "app/src/main/java/" + pkgPath);
        File resDir  = new File(root, "app/src/main/res");
        File layoutDir = new File(resDir, "layout");
        File valuesDir = new File(resDir, "values");
        File drawableDir = new File(resDir, "drawable");

        javaDir.mkdirs();
        layoutDir.mkdirs();
        valuesDir.mkdirs();
        drawableDir.mkdirs();

        // Root files
        writeFile(root, "settings.gradle",    buildSettingsGradle(safe));
        writeFile(root, "build.gradle",        buildRootGradle());
        writeFile(root, "gradle.properties",   buildGradleProperties());
        writeFile(new File(root, "app"), "build.gradle", buildAppGradle(packageName));
        writeFile(new File(root, "app/src/main"), "AndroidManifest.xml", buildManifest(packageName, safe));
        writeFile(valuesDir, "strings.xml",   buildStringsXml(projectName));
        writeFile(valuesDir, "colors.xml",    buildColorsXml());
        writeFile(valuesDir, "themes.xml",    buildThemesXml());
        writeFile(layoutDir, "activity_main.xml", buildMainLayout());

        return root;
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
    }

    public void writeSourceFile(File root, String packageName, String className, String code) throws IOException {
        String pkgPath = packageName.replace(".", "/");
        File   javaDir = new File(root, "app/src/main/java/" + pkgPath);
        javaDir.mkdirs();
        writeFile(javaDir, className + ".java", code);
    }

    public void writeFile(File dir, String name, String content) throws IOException {
        dir.mkdirs();
        File f = new File(dir, name);
        try (FileWriter fw = new FileWriter(f)) {
            fw.write(content);
        }
    }

    public String readFile(File f) throws IOException {
        if (!f.exists()) return "";
        byte[] bytes = Files.readAllBytes(f.toPath());
        return new String(bytes, "UTF-8");
    }

    public boolean deleteProject(String projectName) {
        return deleteDir(new File(projectsRoot, sanitize(projectName)));
    }

    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            for (File child : dir.listFiles()) deleteDir(child);
        }
        return dir.delete();
    }

    public List<String> listFiles(File dir) {
        List<String> list = new ArrayList<>();
        if (dir == null || !dir.exists()) return list;
        traverse(dir, dir.getAbsolutePath(), list);
        return list;
    }

    private void traverse(File f, String rootPath, List<String> list) {
        if (f.isFile()) {
            list.add(f.getAbsolutePath().substring(rootPath.length() + 1));
        } else if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) traverse(c, rootPath, list);
            }
        }
    }

    public File getProjectsRoot() { return projectsRoot; }

    // ── Gradle / manifest templates ───────────────────────────────────────────

    private String buildSettingsGradle(String appName) {
        return "pluginManagement {\n" +
               "    repositories { google(); mavenCentral() }\n}\n" +
               "dependencyResolutionManagement {\n" +
               "    repositories { google(); mavenCentral() }\n}\n" +
               "rootProject.name = \"" + appName + "\"\n" +
               "include ':app'\n";
    }

    private String buildRootGradle() {
        return "buildscript {\n" +
               "    repositories { google(); mavenCentral() }\n" +
               "    dependencies {\n" +
               "        classpath 'com.android.tools.build:gradle:8.0.0'\n" +
               "    }\n}\n" +
               "allprojects {\n" +
               "    repositories { google(); mavenCentral() }\n}\n";
    }

    private String buildGradleProperties() {
        return "android.useAndroidX=true\n" +
               "android.enableJetifier=true\n" +
               "org.gradle.jvmargs=-Xmx2048m\n";
    }

    private String buildAppGradle(String packageName) {
        return "plugins {\n" +
               "    id 'com.android.application'\n}\n\n" +
               "android {\n" +
               "    namespace '" + packageName + "'\n" +
               "    compileSdk 34\n" +
               "    defaultConfig {\n" +
               "        applicationId '" + packageName + "'\n" +
               "        minSdk 29\n" +
               "        targetSdk 34\n" +
               "        versionCode 1\n" +
               "        versionName '1.0'\n" +
               "    }\n" +
               "    buildTypes {\n" +
               "        release { minifyEnabled false }\n" +
               "    }\n}\n\n" +
               "dependencies {\n" +
               "    implementation 'androidx.appcompat:appcompat:1.6.1'\n" +
               "    implementation 'com.google.android.material:material:1.11.0'\n" +
               "    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'\n}\n";
    }

    private String buildManifest(String packageName, String appName) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
               "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
               "    <application\n" +
               "        android:allowBackup=\"true\"\n" +
               "        android:label=\"@string/app_name\"\n" +
               "        android:theme=\"@style/Theme.App\">\n" +
               "        <activity\n" +
               "            android:name=\"." + toCamelCase(appName) + "MainActivity\"\n" +
               "            android:exported=\"true\">\n" +
               "            <intent-filter>\n" +
               "                <action android:name=\"android.intent.action.MAIN\" />\n" +
               "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
               "            </intent-filter>\n" +
               "        </activity>\n" +
               "    </application>\n" +
               "</manifest>\n";
    }

    private String buildStringsXml(String appName) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
               "<resources>\n" +
               "    <string name=\"app_name\">" + appName + "</string>\n" +
               "</resources>\n";
    }

    private String buildColorsXml() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
               "<resources>\n" +
               "    <color name=\"primary\">#6200EE</color>\n" +
               "    <color name=\"primary_dark\">#3700B3</color>\n" +
               "    <color name=\"accent\">#03DAC5</color>\n" +
               "</resources>\n";
    }

    private String buildThemesXml() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
               "<resources>\n" +
               "    <style name=\"Theme.App\" parent=\"Theme.MaterialComponents.DayNight.DarkActionBar\">\n" +
               "        <item name=\"colorPrimary\">@color/primary</item>\n" +
               "    </style>\n" +
               "</resources>\n";
    }

    private String buildMainLayout() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
               "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
               "    android:layout_width=\"match_parent\"\n" +
               "    android:layout_height=\"match_parent\"\n" +
               "    android:orientation=\"vertical\"\n" +
               "    android:gravity=\"center\"\n" +
               "    android:padding=\"24dp\">\n" +
               "    <TextView\n" +
               "        android:layout_width=\"wrap_content\"\n" +
               "        android:layout_height=\"wrap_content\"\n" +
               "        android:text=\"@string/app_name\"\n" +
               "        android:textSize=\"24sp\"\n" +
               "        android:textStyle=\"bold\" />\n" +
               "</LinearLayout>\n";
    }

    private String toCamelCase(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
