package com.example.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class CloudflareException(message: String) : IOException(message)

class CloudflareInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Add User-Agent and Cookie if we have them
        val requestBuilder = originalRequest.newBuilder()
        CloudflareSolver.userAgent?.let { 
            requestBuilder.header("User-Agent", it) 
        }
        CloudflareSolver.cfClearanceCookie?.let { 
            requestBuilder.header("Cookie", it) 
        }
        
        // Bypass hotlink protection for images
        requestBuilder.header("Referer", "https://web1.mgkomik.cc/")

        
        val request = requestBuilder.build()
        val response = chain.proceed(request)

        // Check if Cloudflare blocked us
        if (response.code == 403 || response.code == 503) {
            val serverHeader = response.header("Server") ?: ""
            if (serverHeader.contains("cloudflare", ignoreCase = true)) {
                // Cloudflare challenge detected
                response.close()
                throw CloudflareException("Cloudflare challenge detected. Needs bypass.")
            }
        }
        
        return response
    }
}

object CloudflareSolver {
    var cfClearanceCookie: String? = null
    var userAgent: String? = null
}
