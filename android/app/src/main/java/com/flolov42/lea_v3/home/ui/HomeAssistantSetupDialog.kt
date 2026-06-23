package com.flolov42.lea_v3.home.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.widget.*
import com.flolov42.lea_v3.home.control.HomeAssistantController
import com.flolov42.lea_v3.home.models.HomeConfig

object HomeAssistantSetupDialog {

    interface SetupCallback {
        fun onSaved()
        fun onCancelled()
    }

    @JvmStatic
    fun show(ctx: Context, cb: SetupCallback?) {
        val config = HomeConfig.get(ctx)

        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(ctx, 20)
            setPadding(pad, pad, pad, 8)
        }

        fun label(text: String) = TextView(ctx).apply {
            this.text = text; textSize = 12f; setTextColor(0xFF9CA3AF.toInt())
            setPadding(0, dp(ctx, 8), 0, 2)
        }

        val urlField = EditText(ctx).apply {
            hint = "http://homeassistant.local:8123"
            setText(config.haUrl)
            isSingleLine = true
            setBackgroundColor(0x1AFFFFFF.toInt())
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
        }
        val tokenField = EditText(ctx).apply {
            hint = "Long-Lived Access Token"
            setText(config.haToken)
            isSingleLine = true
            setBackgroundColor(0x1AFFFFFF.toInt())
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
        }
        val statusView = TextView(ctx).apply {
            textSize = 12f; setPadding(0, dp(ctx, 8), 0, 0)
        }
        val testBtn = Button(ctx).apply {
            text = "Tester la connexion"
            setBackgroundColor(0xFF6C63FF.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }
        testBtn.setOnClickListener {
            val url   = urlField.text.toString().trim()
            val token = tokenField.text.toString().trim()
            if (url.isEmpty() || token.isEmpty()) {
                statusView.text = "Remplissez tous les champs"
                statusView.setTextColor(0xFFE53935.toInt())
                return@setOnClickListener
            }
            statusView.text = "Test en cours…"; statusView.setTextColor(0xFF9CA3AF.toInt())
            HomeAssistantController.checkConnectionWith(url, token, object : HomeAssistantController.HaCallback {
                override fun onSuccess(r: String) {
                    statusView.text = "✓ Connexion réussie !"; statusView.setTextColor(0xFF4CAF50.toInt())
                }
                override fun onError(e: String) {
                    statusView.text = "✗ $e"; statusView.setTextColor(0xFFE53935.toInt())
                }
            })
        }

        layout.addView(label("URL du serveur Home Assistant :"))
        layout.addView(urlField)
        layout.addView(label("Token d'accès :"))
        layout.addView(tokenField)
        layout.addView(testBtn)
        layout.addView(statusView)

        AlertDialog.Builder(ctx)
            .setTitle("Configuration Home Assistant")
            .setView(layout)
            .setPositiveButton("Enregistrer") { _, _ ->
                val url   = urlField.text.toString().trim()
                val token = tokenField.text.toString().trim()
                config.setHaUrl(url); config.setHaToken(token)
                config.setHaEnabled(url.isNotEmpty() && token.isNotEmpty())
                cb?.onSaved()
            }
            .setNegativeButton("Annuler") { _, _ -> cb?.onCancelled() }
            .show()
    }

    private fun dp(ctx: Context, v: Int) =
        (v * ctx.resources.displayMetrics.density).toInt()
}
