package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

class PotholeRepository(
    private val api: PotholeApiService = NetworkModule.potholeApiService
) {

    suspend fun getPotholes(status: String? = null, ward: String? = null): ApiResult<List<PotholeDto>> =
        withContext(Dispatchers.IO) {
            try {
                ApiResult.Success(api.getPotholes(status, ward))
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Failed to load potholes")
            }
        }

    suspend fun submitReport(
        lat: Double,
        lng: Double,
        severity: String,
        source: String,
        ward: String? = null,
        photoUrl: String? = null
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
                        photoUrl = photoUrl
                    )
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to submit report")
        }
    }

    suspend fun updateStatus(id: String, status: String): ApiResult<PotholeDto> =
        withContext(Dispatchers.IO) {
            try {
                ApiResult.Success(api.updateStatus(id, UpdateStatusRequest(status)))
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Failed to update status")
            }
        }
}
