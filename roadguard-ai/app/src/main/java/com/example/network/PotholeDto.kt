package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PotholeDto(
    val id: String,
    val lat: Double,
    val lng: Double,
    val severity: String,
    val source: String,
    @Json(name = "photo_url") val photoUrl: String?,
    val status: String,
    val ward: String?,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class CreatePotholeRequest(
    val lat: Double,
    val lng: Double,
    val severity: String,
    val source: String,
    val ward: String?,
    @Json(name = "photo_url") val photoUrl: String?
)

@JsonClass(generateAdapter = true)
data class UpdateStatusRequest(
    val status: String
)
