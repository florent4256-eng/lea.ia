package com.flolov42.lea_v3.bixby

import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode

class LeaModelViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL      = "model_url"
        const val EXTRA_FILENAME = "model_filename"
    }

    private lateinit var sceneView:  SceneView
    private lateinit var card:       FrameLayout
    private lateinit var cardBg:     GradientDrawable
    private lateinit var expandBtn:  TextView
    private var isExpanded = false
    private var screenW = 0
    private var screenH = 0
    private var dp2 = 0; private var dp4 = 0; private var dp8 = 0
    private var dp16 = 0; private var dp36 = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Affichage par-dessus l'écran verrouillé
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true); setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val modelUrl = intent.getStringExtra(EXTRA_URL)
        if (modelUrl.isNullOrEmpty()) { finish(); return }

        val d = resources.displayMetrics.density
        dp2 = (2*d).toInt(); dp4 = (4*d).toInt(); dp8 = (8*d).toInt()
        dp16 = (16*d).toInt(); dp36 = (36*d).toInt()
        val dm = resources.displayMetrics
        screenW = dm.widthPixels; screenH = dm.heightPixels

        // ── OVERLAY SOMBRE ────────────────────────────────────────────
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.parseColor("#B3000000"))
        root.setOnClickListener { finish() }

        // ── CARD CENTREE ──────────────────────────────────────────────
        card = FrameLayout(this)
        card.isClickable = true
        cardBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#CC0F0A1E"))
            cornerRadius = dp16 * 2f
            setStroke(dp2 / 2, Color.parseColor("#33FFFFFF"))
        }
        card.background = cardBg
        root.addView(card, cardNormalLp())

        // ── CONTENU DE LA CARD ────────────────────────────────────────
        val cardContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        card.addView(cardContent, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // ── TOOLBAR ───────────────────────────────────────────────────
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            setBackgroundColor(Color.parseColor("#33000000"))
            setPadding(dp8, dp4, dp8, dp4)
        }
        cardContent.addView(toolbar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp36 + dp4 * 2))

        expandBtn = makeBtn("⤢")
        toolbar.addView(expandBtn, btnLp())
        expandBtn.setOnClickListener { toggleExpand() }

        val dlBtn = makeBtn("↓")
        toolbar.addView(dlBtn, btnLp())
        dlBtn.setOnClickListener { downloadModel(dlBtn, modelUrl) }

        val closeBtn = makeBtn("✕")
        val closeLp = btnLp().apply { setMargins(dp4, 0, 0, 0) }
        toolbar.addView(closeBtn, closeLp)
        closeBtn.setOnClickListener { finish() }

        // Séparateur
        val sep = View(this).apply { setBackgroundColor(Color.parseColor("#1AFFFFFF")) }
        cardContent.addView(sep, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))

        // ── ZONE SCENEVIEW ────────────────────────────────────────────
        val container = FrameLayout(this)
        val containerLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            setMargins(dp4, dp4, dp4, dp4)
        }

        val loader = ProgressBar(this)
        container.addView(loader, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))

        sceneView = SceneView(this)
        sceneView.visibility = View.GONE
        container.addView(sceneView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        cardContent.addView(container, containerLp)
        setContentView(root)

        // ── CHARGEMENT DU MODÈLE GLB ──────────────────────────────────
        try {
            sceneView.modelLoader.loadModelAsync(
                fileLocation = modelUrl,
                onResult = { asset ->
                    runOnUiThread {
                        try {
                            if (asset != null) {
                                val instance = asset.getInstance()
                                val node = ModelNode(
                                    modelInstance = instance,
                                    scaleToUnits = 1.0f
                                )
                                sceneView.addChildNode(node)
                                loader.visibility    = View.GONE
                                sceneView.visibility = View.VISIBLE
                            } else {
                                Toast.makeText(this, "Modèle 3D introuvable", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "Erreur affichage 3D : ${e.message}", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur chargement SceneView : ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────

    private fun makeBtn(label: String) = TextView(this).apply {
        text = label
        setTextColor(Color.parseColor("#94A3B8"))
        textSize = 15f
        gravity = Gravity.CENTER
    }

    private fun btnLp() = LinearLayout.LayoutParams(dp36, dp36).apply {
        setMargins(dp4, 0, dp4, 0)
    }

    private fun cardNormalLp() = FrameLayout.LayoutParams(
        (screenW * 0.82).toInt(), (screenH * 0.58).toInt(), Gravity.CENTER)

    private fun cardExpandedLp() = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT).apply {
        setMargins(dp8, dp8, dp8, dp8)
    }

    private fun toggleExpand() {
        isExpanded = !isExpanded
        if (isExpanded) {
            card.layoutParams = cardExpandedLp()
            cardBg.cornerRadius = dp8 * 2f
            expandBtn.text = "⊟"
        } else {
            card.layoutParams = cardNormalLp()
            cardBg.cornerRadius = dp16 * 2f
            expandBtn.text = "⤢"
        }
    }

    private fun downloadModel(btn: TextView, url: String) {
        val filename = intent.getStringExtra(EXTRA_FILENAME)
            ?: "lea_3d_${System.currentTimeMillis()}.glb"
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(filename)
            .setDescription("Modèle 3D Léa Forge")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Léa 3D/$filename")
            .setAllowedOverMetered(true)
        (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        btn.text = "✓"
        btn.setTextColor(Color.parseColor("#22C55E"))
        Toast.makeText(this, "Téléchargement → Téléchargements/Léa 3D", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::sceneView.isInitialized) sceneView.destroy()
        // Rend le micro à Bixby si elle était en conversation quand la 3D a terminé
        if (intent.getBooleanExtra("give_mic_on_close", false)) {
            LeaNovaService.instance?.triggerCooldownFromActivity()
        }
    }
}
