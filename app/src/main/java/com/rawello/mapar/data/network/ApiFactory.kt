package com.rawello.mapar.data.network

import com.google.gson.GsonBuilder
import com.rawello.mapar.data.models.NodeDb
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import com.rawello.mapar.data.TokenManager

object ApiFactory {
    private const val BASE_URL = "http://194.67.84.115:8881/"

    // Провайдер токена (лямбда), по умолчанию возвращает пустую строку
    private var tokenProvider: (() -> String) = { "" }

    private fun buildOkHttpClient(): OkHttpClient {
        return TokenManager.buildOkHttpClient(tokenProvider)
    }

    val gson = GsonBuilder()
        .registerTypeAdapter(FloatArray::class.java, StringToFloatArrayAdapter())
        .create()

    @Volatile
    private var retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(buildOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Volatile
    var apiService: ApiService = retrofit.create(ApiService::class.java)
        private set

    // Вызывать при смене токена
    fun updateTokenProvider(provider: () -> String) {
        tokenProvider = provider
        // Пересоздаём retrofit и apiService с новым токеном
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(buildOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        apiService = retrofit.create(ApiService::class.java)
    }
}
