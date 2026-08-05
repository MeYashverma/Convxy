/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.music.innertube.YouTube
import com.convx.music.LocalPlayerAwareWindowInsets
import com.convx.music.R
import com.convx.music.constants.DataSyncIdKey
import com.convx.music.constants.InnerTubeCookieKey
import com.convx.music.constants.SavedAccount
import com.convx.music.constants.VisitorDataKey
import com.convx.music.ui.component.IconButton
import com.convx.music.ui.utils.backToMain
import com.convx.music.utils.rememberPreference
import com.convx.music.utils.reportException
import com.convx.music.viewmodels.AccountSettingsViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

/**
 * Holds the cookie/visitorData/channel-list from a completed WebView login while
 * [ChannelPickerScreen] is on screen, when the account has more than one YouTube
 * channel. Transient, in-memory only — if the process dies mid-picker the user
 * just logs in again, same as any other interrupted login.
 */
object PendingChannelLogin {
    var cookie: String = ""
    var visitorData: String = ""
    var channels: List<SavedAccount> = emptyList()
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun LoginScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var visitorData by rememberPreference(VisitorDataKey, "")
    var dataSyncId by rememberPreference(DataSyncIdKey, "")
    var innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    var hasCompletedLogin by remember { mutableStateOf(false) }
    val accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()

    // The JS bridge values land here synchronously. The DataStore-backed prefs
    // above only settle after an async write plus a flow emission, so reading
    // them straight after login raced and could hand YouTube a blank
    // visitorData / dataSyncId — which then breaks search and playback for the
    // whole session, and stays broken because the blanks get persisted.
    val liveVisitorData = remember { AtomicReference("") }
    val liveDataSyncId = remember { AtomicReference("") }

    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .fillMaxSize(),
            factory = { webViewContext ->
                WebView(webViewContext).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                            loadUrl("javascript:Android.onRetrieveDataSyncId(window.yt.config_.DATASYNC_ID)")

                            if (url?.startsWith("https://music.youtube.com") == true && !hasCompletedLogin) {
                                val cookie = CookieManager.getInstance().getCookie(url)
                                if (cookie.isNullOrBlank()) return  // not signed in yet; wait for the next page
                                innerTubeCookie = cookie
                                hasCompletedLogin = true

                                coroutineScope.launch {
                                    // Wait for the async JS bridge instead of guessing with a
                                    // fixed delay — a blank visitorData poisons every later request.
                                    var waitedMs = 0
                                    while (liveVisitorData.get().isBlank() && waitedMs < 5000) {
                                        delay(100)
                                        waitedMs += 100
                                    }
                                    val newVisitorData = liveVisitorData.get()
                                    if (newVisitorData.isBlank()) {
                                        Timber.e("Login: visitorData never arrived — aborting instead of storing a blank session")
                                        hasCompletedLogin = false
                                        return@launch
                                    }

                                    // Initialize YouTube object with new authentication data
                                    YouTube.cookie = cookie
                                    YouTube.dataSyncId = liveDataSyncId.get().ifBlank { null }
                                    YouTube.visitorData = newVisitorData

                                    Timber.d("Login: YouTube object initialized, validating...")

                                    YouTube.accountInfo().onSuccess {
                                        // A Google account can have several YouTube channels — this
                                        // is the only call that lists all of them without opening
                                        // YouTube's own UI. See getAccountChannels for why.
                                        val channels = YouTube.getAccountChannels().getOrDefault(emptyList())
                                            .mapNotNull { c ->
                                                c.dataSyncId?.let { id ->
                                                    SavedAccount(id, c.name, c.channelHandle, c.thumbnailUrl)
                                                }
                                            }

                                        // Clean up WebView
                                        webViewRef.value?.apply {
                                            stopLoading()
                                            clearHistory()
                                            clearCache(true)
                                            clearFormData()
                                        }

                                        if (channels.size > 1) {
                                            Timber.d("Login: ${channels.size} channels on this account, showing picker")
                                            PendingChannelLogin.cookie = cookie
                                            PendingChannelLogin.visitorData = newVisitorData
                                            PendingChannelLogin.channels = channels
                                            navController.navigate("channel_picker") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        } else {
                                            Timber.d("Login: single channel, restarting app...")
                                            accountSettingsViewModel.applyChannelAndRestart(
                                                context = context,
                                                cookie = cookie,
                                                visitorData = newVisitorData,
                                                chosenDataSyncId = channels.firstOrNull()?.dataSyncId
                                                    ?: liveDataSyncId.get(),
                                                allChannels = channels,
                                            )
                                        }
                                    }.onFailure {
                                        Timber.e(it, "Login: Authentication validation failed")
                                        hasCompletedLogin = false // Allow retry
                                        reportException(it)
                                    }
                                }
                            }
                        }
                    }
                    settings.apply {
                        javaScriptEnabled = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                    }
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onRetrieveVisitorData(newVisitorData: String?) {
                            if (!newVisitorData.isNullOrBlank() && newVisitorData != "null") {
                                liveVisitorData.set(newVisitorData)
                                visitorData = newVisitorData
                            }
                        }
                        @JavascriptInterface
                        fun onRetrieveDataSyncId(newDataSyncId: String?) {
                            // Kept whole, not truncated at "||" — that suffix is
                            // what distinguishes a brand/second channel from the
                            // account's primary identity. Stripping it meant the
                            // app could only ever authenticate as the primary
                            // channel no matter which one was active.
                            if (!newDataSyncId.isNullOrBlank() && newDataSyncId != "null") {
                                liveDataSyncId.set(newDataSyncId)
                                dataSyncId = newDataSyncId
                            }
                        }
                    }, "Android")
                    webViewRef.value = this
                    loadUrl("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com")
                }
            }
        )

        TopAppBar(
            title = { Text(stringResource(R.string.login)) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null
                    )
                }
            }
        )
    }

    BackHandler(enabled = webViewRef.value?.canGoBack() == true) {
        webViewRef.value?.goBack()
    }
}
