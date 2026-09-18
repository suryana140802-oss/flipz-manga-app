package com.example.monetization

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Manager monetisasi Monetag (SmartLink / Direct Link).
 * Menyediakan integrasi iklan sponsor mandiri yang kompatibel dengan distribusi APK non-Play Store.
 */
object MonetagManager {
    private const val TAG = "MonetagManager"

    // SmartLink Monetag resmi Flipz Manga
    const val DIRECT_LINK_URL = "https://omg10.com/4/11834728"

    /**
     * Membuka tautan sponsor Monetag menggunakan browser bawaan Android.
     */
    fun openDirectLink(context: Context, url: String = DIRECT_LINK_URL) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "Berhasil membuka Monetag Direct Link: $url")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membuka Monetag Direct Link: ${e.message}", e)
        }
    }
}
