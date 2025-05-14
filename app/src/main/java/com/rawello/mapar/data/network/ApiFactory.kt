package com.rawello.mapar.data.network

import com.google.gson.GsonBuilder
import com.rawello.mapar.data.models.NodeDb
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient

object ApiFactory {
    private const val BASE_URL = "http://194.67.84.115:8881/"
    var TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhbm9ueW1vdXMiLCJjcmVhdGVkIjoxNzMwOTk5MjM1NDIwLCJyZWFkT25seSI6dHJ1ZSwiZXhwIjoyMDkwOTk5MjM1fQ.FYNLHrVEZ14Qh0Tu0BbCOJXVeN65jYiTCSjGReSYDG4VrjZ3slThOyeku5NgiFpmPVBir30mZXubbZ3NUMUxbA"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(TOKEN))
        .build()

    val gson = GsonBuilder()
        .registerTypeAdapter(FloatArray::class.java, StringToFloatArrayAdapter())
        .create()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
