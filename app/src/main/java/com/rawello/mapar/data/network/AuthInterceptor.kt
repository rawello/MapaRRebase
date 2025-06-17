package com.rawello.mapar.data.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenProvider: () -> String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        val method = originalRequest.method.toString()
        val isOwnerRequest = url.contains("/is-owner")
        val isPublic = (
            url.contains("/api/auth/anonymous-token") ||
            url.contains("/api/auth/login") ||
            url.contains("/api/auth/register") ||
            (url.contains("/api/buildings") && method == "GET") ||
            (url.contains("/api/buildings/") && url.contains("/nodes") && method == "GET")
        ) && !isOwnerRequest
        val token = tokenProvider()
        val newRequest = if (!isPublic && token.isNotEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        return chain.proceed(newRequest)
    }
}