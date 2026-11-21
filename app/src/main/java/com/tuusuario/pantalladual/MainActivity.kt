package com.tuusuario.pantalladual

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var displayManager: DisplayManager
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

        val scrollView = ScrollView(this)
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        statusText = TextView(this).apply {
            textSize = 16f
            setPadding(0, 0, 0, 24)
        }
        mainLayout.addView(statusText)

        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val installedApps = pm.queryIntentActivities(intent, 0)
            .sortedBy { it.loadLabel(pm).toString() }

        for (appInfo in installedApps) {
            val appName = appInfo.loadLabel(pm).toString()
            val packageName = appInfo.activityInfo.packageName

            val appButton = Button(this).apply {
                text = "Abrir en TV: $appName"
                setOnClickListener {
                    launchAppOnTv(packageName, appName)
                }
            }
            mainLayout.addView(appButton)
        }

        scrollView.addView(mainLayout)
        setContentView(scrollView)
    }

    override fun onResume() {
        super.onResume()
        updateDisplayStatus()
    }

    private fun updateDisplayStatus() {
        val tvDisplay = getExternalDisplay()
        statusText.text = if (tvDisplay != null) {
            "✅ TV conectada: ${tvDisplay.name}\nToca una app abajo para enviarla a la TV:"
        } else {
            "❌ No hay TV conectada por Screen Mirroring.\nConéctate primero y vuelve aquí."
        }
    }

    private fun getExternalDisplay(): Display? {
        val displays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        return displays.firstOrNull()
    }

    private fun launchAppOnTv(packageName: String, appName: String) {
        val tvDisplay = getExternalDisplay()
        if (tvDisplay == null) {
            Toast.makeText(this, "Conecta el Screen Mirroring primero", Toast.LENGTH_SHORT).show()
            return
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val options = ActivityOptions.makeBasic().apply {
                    launchDisplayId = tvDisplay.displayId
                }
                try {
                    startActivity(launchIntent, options.toBundle())
                    Toast.makeText(this, "Abriendo $appName en la TV...", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Android bloqueó el envío de $appName a la TV", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
