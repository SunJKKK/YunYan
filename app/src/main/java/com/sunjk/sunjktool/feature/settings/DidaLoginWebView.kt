package com.sunjk.sunjktool.feature.settings

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/**
 * 内嵌 WebView 打开滴答清单登录页，登录成功后自动抓取 token。
 *
 * 关键点：滴答登录成功后，认证 token 存放在 **HttpOnly cookie `t`**（Domain=.dida365.com），
 * 而非响应体。JS 无法读取 HttpOnly cookie，但 Android [CookieManager] 可以，因此这里
 * 主要靠轮询 CookieManager 读 `t` cookie；JS localStorage 作为回退。
 *
 * 注意：属非官方方式，可能受验证码/风控影响，需真机验证。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DidaLoginWebView(
    onTokenCaptured: (token: String, csrf: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var captured by remember { mutableStateOf(false) }

    fun readCookieValue(name: String): String? {
        val hosts = listOf(
            "https://dida365.com",
            "https://www.dida365.com",
            "https://api.dida365.com"
        )
        val regex = Regex("(?:^|;\\s*)$name=([^;]+)")
        for (h in hosts) {
            val cookies = try { CookieManager.getInstance().getCookie(h) } catch (_: Exception) { null }
                ?: continue
            val m = regex.find(cookies)
            if (m != null && m.groupValues.getOrNull(1).isNullOrBlank().not()) {
                return m.groupValues[1]
            }
        }
        return null
    }

    fun readToken() {
        if (captured) return
        val t = readCookieValue("t")
        val csrf = readCookieValue("_csrf_token")
        if (t.isNullOrBlank()) return
        captured = true
        onTokenCaptured(t, csrf ?: "")
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
                settings.setSupportZoom(true)
                CookieManager.getInstance().setAcceptCookie(true)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        readToken()
                    }
                }
                webChromeClient = WebChromeClient()
                webView = this
                loadUrl("https://www.dida365.com/")
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { webView = it }
    )

    // 轮询读取 token（登录后可能无整页跳转，需定时检查 cookie）
    LaunchedEffect(Unit) {
        while (!captured) {
            delay(1200)
            readToken()
        }
    }

    // 空占位（提示由外部覆盖层提供），避免空组合
    Box(modifier = Modifier.fillMaxSize()) {}
}
