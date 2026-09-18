package com.example.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Menyimpan dan mengambil Cloudflare bypass cookie dari SharedPreferences
 * agar tidak perlu bypass ulang setiap kali membuka aplikasi.
 */
object CookieCache {
    private const val PREFS_NAME = "cf_cache"
    private const val KEY_COOKIE = "cf_clearance"
    private const val KEY_USER_AGENT = "user_agent"
    // Cookie cf_clearance biasanya berlaku 1 jam (Cloudflare default)
    private const val COOKIE_TTL_MS = 50 * 60 * 1000L // 50 menit untuk aman

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun save(cookie: String, userAgent: String) {
        prefs.edit()
            .putString(KEY_COOKIE, cookie)
            .putString(KEY_USER_AGENT, userAgent)
            .putLong("saved_at", System.currentTimeMillis())
            .apply()
    }

    /** Returns cached cookie jika masih valid, null jika sudah kadaluarsa */
    fun getCachedCookie(): String? {
        val savedAt = prefs.getLong("saved_at", 0L)
        if (System.currentTimeMillis() - savedAt > COOKIE_TTL_MS) return null
        return prefs.getString(KEY_COOKIE, null)
    }

    fun getCachedUserAgent(): String? {
        return prefs.getString(KEY_USER_AGENT, null)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
