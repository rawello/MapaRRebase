package com.rawello.mapar.data.network

import com.rawello.mapar.data.models.Building
import com.rawello.mapar.data.models.CreateBuildingRequest
import com.rawello.mapar.data.models.JwtAuthenticationResponse
import com.rawello.mapar.data.models.LoginRequest
import com.rawello.mapar.data.models.NodeDb
import com.rawello.mapar.data.models.SignUpRequest
import retrofit2.http.*

interface ApiService {
    @GET("api/buildings")
    suspend fun getAllBuildings(): List<Building>

    @GET("api/buildings/{id}/nodes")
    suspend fun getNodesForBuilding(@Path("id") buildingId: Long): List<NodeResponse>

    @GET("api/auth/anonymous-token")
    suspend fun getAnonymousToken(): JwtAuthenticationResponse

    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): JwtAuthenticationResponse

    @POST("api/auth/register")
    suspend fun register(@Body signUpRequest: SignUpRequest): String

    @GET("api/auth/validate-token")
    suspend fun validateToken(): Boolean

    @POST("api/users/favorites")
    suspend fun updateFavorites(@Body favorites: List<Long>)

    @GET("api/users/favorites")
    suspend fun getFavorites(): List<Long>

    @GET("api/auth/is-anonymous")
    suspend fun isAnonymousUser(): Boolean

    @GET("api/buildings/{id}/is-owner")
    suspend fun isBuildingOwner(@Path("id") buildingId: Long): Boolean

    @POST("api/buildings")
    suspend fun createBuilding(@Body building: CreateBuildingRequest): Building

    @PUT("api/buildings/{id}")
    suspend fun updateBuilding(@Path("id") id: Long, @Body building: Building): Building

    @POST("api/nodes")
    suspend fun createNode(@Body node: NodeResponse): NodeResponse

    @PUT("api/nodes/{id}")
    suspend fun updateNode(@Path("id") id: Long, @Body node: NodeResponse): NodeResponse

    @DELETE("api/nodes/{id}")
    suspend fun deleteNode(@Path("id") id: Long)

    @POST("api/buildings/{buildingId}/nodes")
    suspend fun createNodeForBuilding(
        @Path("buildingId") buildingId: Long,
        @Body node: NodeResponse
    ): NodeResponse

    @PUT("api/buildings/{buildingId}/nodes/{nodeId}")
    suspend fun updateNodeForBuilding(
        @Path("buildingId") buildingId: Long,
        @Path("nodeId") nodeId: Long,
        @Body node: NodeResponse
    ): NodeResponse

    @DELETE("api/buildings/{buildingId}/nodes/{nodeId}")
    suspend fun deleteNodeForBuilding(
        @Path("buildingId") buildingId: Long,
        @Path("nodeId") nodeId: Long
    )
}