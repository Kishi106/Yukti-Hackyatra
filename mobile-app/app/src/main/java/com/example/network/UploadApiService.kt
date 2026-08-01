package com.example.network

import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

@JsonClass(generateAdapter = true)
data class UploadResponse(val url: String)

interface UploadApiService {

    @Multipart
    @POST("uploads")
    suspend fun uploadPhoto(@Part photo: MultipartBody.Part): UploadResponse
}
