
package com.wakaunduvibez.studio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var pendingWebPermission: PermissionRequest? = null

    private val websiteUrl =
        "https://wakaunduvibez-coder.github.io/vibez-studio/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = true

        webView.webViewClient = WebViewClient()

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    val trustedHost =
                        request.origin.host == "wakaunduvibez-coder.github.io"

                    val wantsMicrophone = request.resources.contains(
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE
                    )

                    if (!trustedHost || !wantsMicrophone) {
                        request.deny()
                        return@runOnUiThread
                    }

                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        request.grant(
                            arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                        )
                    } else {
                        pendingWebPermission = request
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.RECORD_AUDIO),
                            100
                        )
                    }
                }
            }
        }

        setContentView(webView)
        webView.loadUrl(websiteUrl)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )

        if (requestCode == 100) {
            val request = pendingWebPermission
            pendingWebPermission = null

            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                request?.grant(
                    arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                )
            } else {
                request?.deny()
                Toast.makeText(
                    this,
                    "Microphone permission is required to record vocals.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        pendingWebPermission?.deny()
        pendingWebPermission = null
        webView.destroy()
        super.onDestroy()
    }
}
