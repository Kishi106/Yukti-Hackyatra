package com.example.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface UserApiService {

    @POST("users")
    suspend fun createUser(@Body body: CreateUserRequest): UserDto

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): UserDto

    @PATCH("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body body: UpdateUserRequest): UserDto
}
