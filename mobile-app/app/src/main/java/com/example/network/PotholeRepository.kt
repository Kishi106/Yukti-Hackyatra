package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

class PotholeRepository(
    private val api: PotholeApiService = NetworkModule.potholeApiService,
    private val uploadApi: UploadApiService = NetworkModule.uploadApiService
) {

    suspend fun getPotholes(status: String? = null, ward: String? = null): ApiResult<List<PotholeDto>> =
        withContext(Dispatchers.IO) {
            try {
                ApiResult.Success(api.getPotholes(status, ward))
            } catch (e: Exception) {
                ApiResult.Error(e.toUserMessage("Failed to load potholes"))
            }
        }

    suspend fun submitReport(
        lat: Double,
        lng: Double,
        severity: String,
        source: String,
        ward: String? = null,
        photoUrl: String? = null,
        reporterId: String? = null
    ): ApiResult<PotholeDto> = withContext(Dispatchers.IO) {
        try {
            ApiResult.Success(
                api.createPothole(
                    CreatePotholeRequest(
                        lat = lat,
                        lng = lng,
                        severity = severity,
                        source = source,
                        ward = ward,
                        photoUrl = photoUrl,
                        reporterId = reporterId
                    )
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.toUserMessage("Failed to submit report"))
        }
    }

    suspend fun updateStatus(id: String, status: String): ApiResult<PotholeDto> =
        withContext(Dispatchers.IO) {
            try {
                ApiResult.Success(api.updateStatus(id, UpdateStatusRequest(status)))
            } catch (e: Exception) {
                ApiResult.Error(e.toUserMessage("Failed to update status"))
            }
        }

    suspend fun confirmPothole(id: String): ApiResult<PotholeDto> = withContext(Dispatchers.IO) {
        try {
            ApiResult.Success(api.confirmPothole(id))
        } catch (e: Exception) {
            ApiResult.Error(e.toUserMessage("Failed to confirm pothole"))
        }
    }

    suspend fun uploadPhoto(imageBytes: ByteArray, filename: String): ApiResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("photo", filename, requestBody)
                ApiResult.Success(uploadApi.uploadPhoto(part).url)
            } catch (e: Exception) {
                ApiResult.Error(e.toUserMessage("Failed to upload photo"))
            }
        }
}
