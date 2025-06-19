package com.rawello.mapar.data.network

import com.rawello.mapar.data.models.Building
import com.rawello.mapar.data.models.JwtAuthenticationResponse
import com.rawello.mapar.data.models.LoginRequest
import com.rawello.mapar.data.models.NodeDb
import com.rawello.mapar.data.models.SignUpRequest
import retrofit2.http.*

interface ApiService {
    // Публичные эндпоинты
    @GET("api/buildings")
    suspend fun getAllBuildings(): List<Building>

    @GET("api/buildings/{id}/nodes")
    suspend fun getNodesForBuilding(@Path("id") buildingId: Long): List<NodeResponse>

    @GET("api/auth/anonymous-token")
    suspend fun getAnonymousToken(): JwtAuthenticationResponse

    // Аутентификация
    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): JwtAuthenticationResponse

    @POST("api/auth/register")
    suspend fun register(@Body signUpRequest: SignUpRequest): String

    @GET("api/auth/validate-token")
    suspend fun validateToken(): Boolean

    // Защищенные эндпоинты 
    @POST("api/users/favorites")
    suspend fun updateFavorites(@Body favorites: List<Long>)

    @GET("api/users/favorites")
    suspend fun getFavorites(): List<Long>

    @GET("api/auth/is-anonymous")
    suspend fun isAnonymousUser(): Boolean

    @GET("api/buildings/{id}/is-owner")
    suspend fun isBuildingOwner(@Path("id") buildingId: Long): Boolean

    // Эндпоинты для владельцев зданий
    @POST("api/buildings")
    suspend fun createBuilding(@Body building: Building): Building

    @PUT("api/buildings/{id}")
    suspend fun updateBuilding(@Path("id") id: Long, @Body building: Building): Building

    @POST("api/nodes")
    suspend fun createNode(@Body node: NodeResponse): NodeResponse

    @PUT("api/nodes/{id}")
    suspend fun updateNode(@Path("id") id: Long, @Body node: NodeResponse): NodeResponse
}