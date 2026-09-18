package com.example.monetization

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.adlisteners.VideoListener

/**
 * Manager monetisasi iklan menggunakan Start.io (StartApp) SDK.
 * 100% gratis, tanpa biaya akun, resmi mendukung distribusi APK mandiri / non-Play Store.
 */
object StartIoAdsManager {
    private const val TAG = "StartIoAdsManager"

    // App ID Start.io resmi milik Flipz Manga
    var APP_ID = "208845479"

    var isInitialized = false
        private set

    // Cooldown iklan interstitial otomatis: minimal jeda 30 detik
    private var lastInterstitialShownTime: Long = 0
    private const val MIN_INTERSTITIAL_INTERVAL_MS = 30_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun postToast(context: Context, message: String) {
        mainHandler.post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Inisialisasi Start.io SDK.
     */
    fun initialize(context: Context, appId: String = APP_ID, testMode: Boolean = false) {
        if (isInitialized) return

        APP_ID = appId
        Log.i(TAG, "🚀 Inisialisasi Start.io SDK (App ID: $APP_ID, Test Mode: $testMode)...")

        try {
            // Inisialisasi SDK, parameter false = nonaktifkan return ads saat membuka lockscreen/kembali ke app
            StartAppSDK.init(context.applicationContext, APP_ID, false)
            StartAppSDK.setTestAdsEnabled(testMode)
            isInitialized = true
            Log.i(TAG, "✅ Start.io SDK berhasil diinisialisasi!")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Gagal inisialisasi Start.io SDK: ${e.message}", e)
        }
    }

    /**
     * Menampilkan iklan reward dukungan (100% Monetag SmartLink).
     */
    fun showRewarded(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit = {}
    ) {
        MonetagManager.openDirectLink(activity)
        mainHandler.post { onRewardEarned() }
        onAdClosed()
    }

    // Counter jumlah chapter yang dibuka
    private var chaptersOpenedCount: Int = 0
    private const val CHAPTERS_BETWEEN_ADS = 2

    // Toggle giliran network iklan: false = Start.io, true = Monetag
    private var isMonetagTurn: Boolean = false

    /**
     * Dipanggil setiap kali pembaca membuka chapter.
     * Iklan hanya akan muncul tepat setiap setelah membuka 2 chapter (misal: chapter 2, 4, 6, dst).
     * Iklan muncul bergantian antara Start.io dan Monetag SmartLink.
     */
    fun onChapterOpened(
        activity: Activity,
        onContinueToChapter: () -> Unit
    ) {
        chaptersOpenedCount++
        Log.i(TAG, "📖 Membuka chapter ke-$chaptersOpenedCount (Target iklan: tiap $CHAPTERS_BETWEEN_ADS chapter)")

        if (chaptersOpenedCount % CHAPTERS_BETWEEN_ADS == 0) {
            val useMonetag = isMonetagTurn
            isMonetagTurn = !isMonetagTurn // Bergantian untuk interval berikutnya

            if (useMonetag) {
                Log.i(TAG, "🎯 Giliran iklan Monetag (SmartLink)...")
                postToast(activity, "Membuka sponsor Flipz Manga...")
                MonetagManager.openDirectLink(activity)
                onContinueToChapter()
            } else {
                Log.i(TAG, "🎯 Giliran iklan Start.io (Interstitial)...")
                showInterstitial(activity, forceShow = true, onAdClosed = onContinueToChapter)
            }
        } else {
            Log.d(TAG, "Lanjut membaca tanpa iklan (belum mencapai $CHAPTERS_BETWEEN_ADS chapter)")
            onContinueToChapter()
        }
    }

    /**
     * Menampilkan iklan Interstitial saat berganti chapter / keluar dari reader.
     */
    fun showInterstitial(
        activity: Activity,
        forceShow: Boolean = false,
        onAdClosed: () -> Unit = {}
    ) {
        val now = System.currentTimeMillis()
        if (!forceShow && (now - lastInterstitialShownTime < MIN_INTERSTITIAL_INTERVAL_MS)) {
            Log.d(TAG, "Lewati iklan interstitial (cooldown aktif)")
            onAdClosed()
            return
        }

        if (!isInitialized) {
            initialize(activity)
        }

        var callbackTriggered = false
        fun safeOnClosed() {
            if (!callbackTriggered) {
                callbackTriggered = true
                mainHandler.post { onAdClosed() }
            }
        }

        // Safety timeout: jika ad network tidak merespons dalam 3.5 detik, lanjutkan membaca
        val timeoutRunnable = Runnable {
            if (!callbackTriggered) {
                Log.w(TAG, "Iklan interstitial timeout (3.5s), lanjut membaca.")
                safeOnClosed()
            }
        }
        mainHandler.postDelayed(timeoutRunnable, 3500L)

        val interstitialAd = StartAppAd(activity)
        interstitialAd.loadAd(StartAppAd.AdMode.AUTOMATIC, object : AdEventListener {
            override fun onReceiveAd(ad: Ad) {
                mainHandler.removeCallbacks(timeoutRunnable)
                interstitialAd.showAd(object : AdDisplayListener {
                    override fun adDisplayed(ad: Ad?) {
                        lastInterstitialShownTime = System.currentTimeMillis()
                    }

                    override fun adHidden(ad: Ad?) {
                        safeOnClosed()
                    }

                    override fun adClicked(ad: Ad?) {}

                    override fun adNotDisplayed(ad: Ad?) {
                        safeOnClosed()
                    }
                })
            }

            override fun onFailedToReceiveAd(ad: Ad?) {
                mainHandler.removeCallbacks(timeoutRunnable)
                safeOnClosed()
            }
        })
    }
}
