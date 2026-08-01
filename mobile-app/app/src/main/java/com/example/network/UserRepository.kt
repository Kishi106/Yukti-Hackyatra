package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

sealed class CreateUserResult {
    data class Success(val user: UserDto) : CreateUserResult()
    data class Conflict(val existingUserId: String?, val message: String) : CreateUserResult()
    data class Error(val message: String) : CreateUserResult()
}

class UserRepository(
    private val api: UserApiService = NetworkModule.userApiService
) {
    private val conflictAdapter = NetworkModule.moshiInstance.adapter(UserConflictError::class.java)

    suspend fun createUser(name: String, phone: String, ward: String?): CreateUserResult =
        withContext(Dispatchers.IO) {
            try {
                CreateUserResult.Success(api.createUser(CreateUserRequest(name, phone, ward)))
            } catch (e: HttpException) {
                if (e.code() == 409) {
                    val body = e.response()?.errorBody()?.string()
                    val parsed = body?.let { runCatching { conflictAdapter.fromJson(it) }.getOrNull() }
                    CreateUserResult.Conflict(
                        existingUserId = parsed?.existingUserId,
                        message = parsed?.error ?: "Account already exists for this phone number"
                    )
                } else {
                    CreateUserResult.Error(e.toUserMessage("Failed to create account"))
                }
            } catch (e: Exception) {
                CreateUserResult.Error(e.toUserMessage("Failed to create account"))
            }
        }

    suspend fun getUser(id: String): ApiResult<UserDto> = withContext(Dispatchers.IO) {
        try {
            ApiResult.Success(api.getUser(id))
        } catch (e: Exception) {
            ApiResult.Error(e.toUserMessage("Failed to load user"))
        }
    }

    suspend fun updateUser(
        id: String,
        name: String? = null,
        phone: String? = null,
        ward: String? = null
    ): ApiResult<UserDto> = withContext(Dispatchers.IO) {
        try {
            ApiResult.Success(api.updateUser(id, UpdateUserRequest(name, phone, ward)))
        } catch (e: Exception) {
            ApiResult.Error(e.toUserMessage("Failed to update user"))
        }
    }
}
