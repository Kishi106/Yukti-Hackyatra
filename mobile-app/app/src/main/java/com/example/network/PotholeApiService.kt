package com.example.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PotholeApiService {

    @GET("potholes")
    suspend fun getPotholes(
        @Query("status") status: String? = null,
        @Query("ward") ward: String? = null
    ): List<PotholeDto>

    @POST("potholes")
    suspend fun createPothole(@Body body: CreatePotholeRequest): PotholeDto

    @PATCH("potholes/{id}")
    suspend fun updateStatus(
        @Path("id") id: String,
        @Body body: UpdateStatusRequest
    ): PotholeDto

    @PATCH("potholes/{id}/confirm")
    suspend fun confirmPothole(@Path("id") id: String): PotholeDto
}
