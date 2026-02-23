package id.sis.exambrowser

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import android.util.Log
import android.app.AlertDialog
import android.widget.EditText
import android.text.InputType
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import id.sis.exambrowser.databinding.ActivityExamBinding

class ExamActivity : ComponentActivity() {

    private lateinit var binding: ActivityExamBinding
    private var backPressCount = 0
    private var lastBackPressedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        secureWindow()

        binding = ActivityExamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFullscreen()
        configureWebView()
        loadExamPage()
    }

    private fun secureWindow() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    private fun setupFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    private fun configureWebView() {
        val settings = binding.webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true

        binding.webView.addJavascriptInterface(
            ExamJsBridge { type, meta ->
                Log.d("ExamEvent", "fromJs type=$type meta=$meta")
            },
            "AndroidExam"
        )

        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val host = request.url.host.orEmpty()
                val allowed = ExamConfig.allowedHost(this@ExamActivity)
                return allowed.isNotEmpty() && host != allowed
            }

            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                sendEventToPage("CLIENT_ERROR", """{"code":$errorCode}""")
            }
        }
    }

    private fun loadExamPage() {
        val token = intent.getStringExtra(EXTRA_SESSION_TOKEN).orEmpty()
        val url = ExamConfig.buildExamUrl(this, token)
        binding.webView.loadUrl(url)
    }

    override fun onResume() {
        super.onResume()
        sendEventToPage("FOCUS_GAINED", "{}")
        startExamLockTask()
    }

    override fun onPause() {
        sendEventToPage("FOCUS_LOST", "{}")
        super.onPause()
    }

    override fun onBackPressed() {
        val now = System.currentTimeMillis()
        if (now - lastBackPressedAt > 3000L) {
            backPressCount = 0
        }
        lastBackPressedAt = now

        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
            return
        }

        backPressCount += 1
        if (backPressCount >= 3) {
            backPressCount = 0
            showAdminPinDialog()
        }
    }

    private fun sendEventToPage(type: String, metaJson: String) {
        val script = "window.sisExamClientEvent && window.sisExamClientEvent('$type', $metaJson);"
        binding.webView.evaluateJavascript(script, null)
    }

    private class ExamJsBridge(
        val onEvent: (type: String, meta: String?) -> Unit
    ) {
        @JavascriptInterface
        fun sendEvent(type: String, metaJson: String?) {
            onEvent(type, metaJson)
        }
    }

    companion object {
        const val EXTRA_SESSION_TOKEN = "extra_session_token"
        private const val ADMIN_PIN = "1234"
    }

    private fun startExamLockTask() {
        try {
            startLockTask()
        } catch (_: IllegalStateException) {
        }
    }

    private fun stopExamLockTask() {
        try {
            stopLockTask()
        } catch (_: IllegalStateException) {
        }
    }

    private fun showAdminPinDialog() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle("Admin PIN")
            .setView(input)
            .setPositiveButton("OK") { dialog, _ ->
                val value = input.text?.toString()?.trim().orEmpty()
                if (value == ADMIN_PIN) {
                    sendEventToPage("ADMIN_EXIT", "{}")
                    stopExamLockTask()
                    finish()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
}
