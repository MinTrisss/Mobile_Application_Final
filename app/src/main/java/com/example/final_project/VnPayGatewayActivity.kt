package com.example.final_project

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class VnPayGatewayActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private val RETURN_URL = "https://my-app.com/vnpay_return"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vnpay_gateway)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        webView = findViewById(R.id.webViewVnPay)
        setupWebView()

        val paymentUrl = intent.getStringExtra("paymentUrl")
        if (paymentUrl != null) {
            webView.loadUrl(paymentUrl)
        } else {
            finish()
        }
    }

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            // Sử dụng shouldOverrideUrlLoading để chặn và xử lý chuyển hướng
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()

                if (url.startsWith(RETURN_URL)) {
                    handleReturnUrl(url)
                    return true // Báo cho WebView rằng chúng ta đã xử lý URL này
                }
                return super.shouldOverrideUrlLoading(view, request) // Để WebView tự xử lý các URL khác
            }
        }
    }

    private fun handleReturnUrl(url: String) {
        val uri = Uri.parse(url)
        val responseCode = uri.getQueryParameter("vnp_ResponseCode")

        if (responseCode == "00") {
            // Giao dịch thành công
            setResult(Activity.RESULT_OK)
        } else {
            // Giao dịch thất bại hoặc bị hủy
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
