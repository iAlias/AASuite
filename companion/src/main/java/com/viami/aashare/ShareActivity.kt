package com.viami.aashare

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL

/** Share-sheet target: POSTs the shared text to the car phone (hotspot gateway). */
class ShareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (text.isNullOrBlank()) {
            Toast.makeText(this, R.string.no_text, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Thread {
            val ok = send(text)
            runOnUiThread {
                Toast.makeText(
                    this,
                    if (ok) R.string.sent_ok else R.string.send_failed,
                    Toast.LENGTH_LONG,
                ).show()
                finish()
            }
        }.start()
    }

    private fun send(text: String): Boolean = try {
        val gateway = gatewayAddress() ?: return false
        val connection =
            URL("http://$gateway:8977/open").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 3000
        connection.readTimeout = 3000
        connection.outputStream.use { it.write(text.toByteArray()) }
        val ok = connection.responseCode == 200
        connection.disconnect()
        ok
    } catch (e: Exception) {
        false
    }

    private fun gatewayAddress(): String? {
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        return GatewayAddress.format(wifi.dhcpInfo?.gateway ?: 0)
    }
}
