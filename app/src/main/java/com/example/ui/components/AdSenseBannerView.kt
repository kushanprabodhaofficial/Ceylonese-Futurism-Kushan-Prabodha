package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val BLOG_BASE_URL = "https://kushanprabodha.blogspot.com/"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdSenseBannerView(
    adClient: String?,
    adSlot: String?,
    rawInsHtml: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOnline = remember { checkNetworkAvailable(context) }
    val normalizedClient = adClient?.trim().orEmpty()
    val hasRawIns = !rawInsHtml.isNullOrBlank() && rawInsHtml.contains("adsbygoogle", ignoreCase = true)

    // Collapse completely when offline or when blog has no AdSense publisher/tag yet
    if (!isOnline || (normalizedClient.isBlank() && !hasRawIns)) {
        return
    }

    var measuredHeightDp by remember(normalizedClient, adSlot, rawInsHtml) { mutableIntStateOf(0) }
    var adFilled by remember(normalizedClient, adSlot, rawInsHtml) { mutableIntStateOf(0) } // 0=pending, 1=filled, -1=unfilled

    val htmlPayload = remember(normalizedClient, adSlot, rawInsHtml) {
        buildAdSenseHtml(
            adClient = normalizedClient,
            adSlot = adSlot?.trim().orEmpty(),
            rawInsHtml = rawInsHtml
        )
    }

    AnimatedVisibility(
        visible = adFilled != -1,
        modifier = modifier
    ) {
        val visibleHeight = measuredHeightDp.coerceIn(0, 280)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = if (visibleHeight > 12) 0.7f else 0f),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("adsense_banner_slot")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (visibleHeight > 16) {
                    Text(
                        text = "දැන්වීම • Ad",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.padding(start = 10.dp, top = 6.dp)
                    )
                }

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (visibleHeight > 0) visibleHeight.dp else 1.dp, max = 280.dp),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadsImagesAutomatically = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            isVerticalScrollBarEnabled = false
                            isHorizontalScrollBarEnabled = false

                            addJavascriptInterface(
                                object {
                                    @JavascriptInterface
                                    fun onAdStatus(status: String, heightPx: Int) {
                                        post {
                                            when (status) {
                                                "filled" -> {
                                                    adFilled = 1
                                                    val density = ctx.resources.displayMetrics.density.coerceAtLeast(1f)
                                                    measuredHeightDp = (heightPx / density).toInt().coerceIn(50, 280)
                                                }
                                                "unfilled" -> {
                                                    adFilled = -1
                                                    measuredHeightDp = 0
                                                }
                                            }
                                        }
                                    }
                                },
                                "AdBridge"
                            )

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    if (url.startsWith("http://") || url.startsWith("https://")) {
                                        runCatching {
                                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }
                                        return true
                                    }
                                    return false
                                }
                            }

                            loadDataWithBaseURL(
                                BLOG_BASE_URL,
                                htmlPayload,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    }
                )
            }
        }
    }
}

private fun buildAdSenseHtml(
    adClient: String,
    adSlot: String,
    rawInsHtml: String?
): String {
    val scriptSrc = if (adClient.isNotBlank()) {
        "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=$adClient"
    } else {
        "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"
    }

    val insMarkup = if (!rawInsHtml.isNullOrBlank() && rawInsHtml.contains("<ins", ignoreCase = true)) {
        rawInsHtml
    } else {
        val slotAttr = if (adSlot.isNotBlank()) """data-ad-slot="$adSlot"""" else ""
        """
        <ins class="adsbygoogle"
             style="display:block;width:100%;"
             data-ad-client="$adClient"
             $slotAttr
             data-ad-format="auto"
             data-full-width-responsive="true"></ins>
        """.trimIndent()
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" />
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    background: transparent;
                    overflow: hidden;
                }
            </style>
            <script async src="$scriptSrc" crossorigin="anonymous"></script>
        </head>
        <body>
            $insMarkup
            <script>
                (function() {
                    try {
                        (window.adsbygoogle = window.adsbygoogle || []).push({});
                    } catch (e) {}
                    var checks = 0;
                    var timer = setInterval(function() {
                        checks++;
                        var ins = document.querySelector('ins.adsbygoogle');
                        if (ins) {
                            var status = ins.getAttribute('data-ad-status');
                            var h = ins.offsetHeight || document.body.scrollHeight || 0;
                            if (status === 'filled' || h >= 40) {
                                if (window.AdBridge) window.AdBridge.onAdStatus('filled', h);
                                clearInterval(timer);
                                return;
                            }
                            if (status === 'unfilled') {
                                if (window.AdBridge) window.AdBridge.onAdStatus('unfilled', 0);
                                clearInterval(timer);
                                return;
                            }
                        }
                        if (checks >= 16) {
                            if (window.AdBridge) window.AdBridge.onAdStatus('unfilled', 0);
                            clearInterval(timer);
                        }
                    }, 450);
                })();
            </script>
        </body>
        </html>
    """.trimIndent()
}

private fun checkNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
