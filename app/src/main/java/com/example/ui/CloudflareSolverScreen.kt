package com.example.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.network.CloudflareSolver
import com.example.network.CookieCache

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CloudflareSolverScreen(
    url: String = "https://web1.mgkomik.cc/",
    onSolved: () -> Unit
) {
    val colors = com.example.ui.theme.LocalAppColors.current

    // Overlay tersembunyi — user tidak melihat WebView
    Box(modifier = Modifier.fillMaxSize()) {

        // WebView di belakang, tidak visible tapi tetap berjalan
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    CloudflareSolver.userAgent = settings.userAgentString

                    webViewClient = object : WebViewClient() {

                        private val adDomains = listOf(
                            "googleads", "doubleclick.net", "popunder", "propellerads",
                            "adsterra", "exoclick", "juicyads", "traffichaus", "hilltopads",
                            "histats", "analytics", "facebook", "shopee"
                        )

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val reqUrl = request?.url?.toString()?.lowercase() ?: ""
                            val isAd = adDomains.any { reqUrl.contains(it) }

                            if (isAd) {
                                return WebResourceResponse(
                                    "text/plain",
                                    "UTF-8",
                                    ByteArrayInputStream("".toByteArray())
                                )
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val reqUrl = request?.url?.toString() ?: ""
                            if (!reqUrl.contains("mgkomik") && !reqUrl.contains("cloudflare")) {
                                return true // Block redirect ke situs lain
                            }
                            return false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val cookies = CookieManager.getInstance().getCookie(url)
                            val title = view?.title ?: ""

                            val isSolved = (!title.contains("Just a moment", ignoreCase = true) &&
                                    !title.contains("Cloudflare", ignoreCase = true)) ||
                                    (cookies != null && cookies.contains("cf_clearance"))

                            if (isSolved) {
                                CloudflareSolver.cfClearanceCookie = cookies ?: ""
                                // Simpan cookie ke cache agar tidak perlu bypass lagi
                                val ua = CloudflareSolver.userAgent ?: ""
                                if (!cookies.isNullOrBlank()) {
                                    CookieCache.save(cookies, ua)
                                }
                                onSolved()
                            }
                        }
                    }
                    loadUrl(url)
                }
            }
        )

        // Overlay loading screen — menutupi WebView sepenuhnya
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.deskDark),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                CircularProgressIndicator(
                    color = colors.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Menghubungkan...",
                    color = colors.textSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Mempersiapkan konten manga",
                    color = colors.textMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}
