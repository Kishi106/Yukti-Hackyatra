package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    val name: String,
    val phone: String,
    val ward: String?,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class CreateUserRequest(
    val name: String,
    val phone: String,
    val ward: String?
)

// Fields left null are omitted by Moshi when serializing (JsonWriter.serializeNulls
// defaults to false), so this doubles as the partial-update body PATCH /users/:id expects.
@JsonClass(generateAdapter = true)
data class UpdateUserRequest(
    val name: String? = null,
    val phone: String? = null,
    val ward: String? = null
)

@JsonClass(generateAdapter = true)
data class UserConflictError(
    val error: String,
    @Json(name = "existingUserId") val existingUserId: String?
)
