package com.flolov42.lea_v3.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class LeaModelViewerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(0xFF000308);

        String modelUrl = getIntent().getStringExtra("modelUrl");
        if (modelUrl == null) { finish(); return; }

        WebView wv = new WebView(this);
        wv.setBackgroundColor(0xFF000308);
        WebSettings ws = wv.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        wv.setWebChromeClient(new WebChromeClient());
        setContentView(wv);

        String html = "<!DOCTYPE html><html><head>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>*{margin:0;padding:0}body{background:#000308;display:flex;flex-direction:column;height:100vh}"
            + "model-viewer{width:100%;flex:1}#hdr{background:#010a17;color:#22d3ee;font-family:sans-serif;"
            + "font-size:12px;padding:10px 16px;cursor:pointer;user-select:none}</style>"
            + "<script type='module' src='https://ajax.googleapis.com/ajax/libs/model-viewer/3.5.0/model-viewer.min.js'></script>"
            + "</head><body>"
            + "<div id='hdr' onclick='history.back()'>← Retour</div>"
            + "<model-viewer src='" + escapeHtml(modelUrl) + "' auto-rotate camera-controls"
            + " environment-image='neutral' shadow-intensity='1' ar ar-modes='webxr scene-viewer quick-look'"
            + " style='background:#000308'></model-viewer>"
            + "</body></html>";
        wv.loadDataWithBaseURL(modelUrl, html, "text/html", "utf-8", null);
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("'", "&#39;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
