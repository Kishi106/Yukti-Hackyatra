package com.example.viewmodels

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.repository.ProfileDto
import com.example.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserProfileState(
    val id: String = "",
    val name: String = "Citizen User",
    val email: String = "user@example.com",
    val phone: String = "+91 98765 43210",
    val username: String = "citizen_user",
    val avatarUrl: String? = null,
    val photoUri: Uri? = null,
    val photoBitmap: Bitmap? = null,
    val createdAt: String = "2026-01-01",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _profile = MutableStateFlow(UserProfileState())
    val profile: StateFlow<UserProfileState> = _profile.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        val userId = repository.getCurrentUserId()
        val email = repository.getCurrentUserEmail() ?: "user@example.com"

        if (userId.isNullOrBlank()) {
            _profile.value = _profile.value.copy(
                email = email,
                name = email.substringBefore("@").replace(".", " ").capitalize()
            )
            return
        }

        viewModelScope.launch {
            _profile.value = _profile.value.copy(isLoading = true, errorMessage = null)
            val result = repository.getProfile(userId)
            result.onSuccess { dto ->
                _profile.value = _profile.value.copy(
                    id = dto.id,
                    name = dto.full_name?.takeIf { it.isNotBlank() } ?: email.substringBefore("@"),
                    email = dto.email?.takeIf { it.isNotBlank() } ?: email,
                    phone = dto.phone ?: "",
                    username = dto.username ?: email.substringBefore("@"),
                    avatarUrl = dto.avatar_url,
                    createdAt = dto.created_at?.take(10) ?: "2026-01-01",
                    isLoading = false
                )
            }.onFailure { err ->
                _profile.value = _profile.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load profile: ${err.localizedMessage}"
                )
            }
        }
    }

    fun updateProfile(
        name: String,
        phone: String,
        username: String,
        imageBytes: ByteArray? = null,
        photoUri: Uri? = null,
        photoBitmap: Bitmap? = null,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        val trimmedUsername = username.trim()

        if (trimmedName.isEmpty()) {
            onComplete(false, "Full Name cannot be empty")
            return
        }

        if (trimmedPhone.isNotEmpty() && !trimmedPhone.matches(Regex("^[+]?[0-9\\s-]{8,15}$"))) {
            onComplete(false, "Please enter a valid phone number")
            return
        }

        val userId = repository.getCurrentUserId() ?: _profile.value.id
        val currentEmail = repository.getCurrentUserEmail() ?: _profile.value.email

        if (userId.isBlank()) {
            onComplete(false, "User session not found")
            return
        }

        viewModelScope.launch {
            _profile.value = _profile.value.copy(isLoading = true, errorMessage = null)

            var newAvatarUrl = _profile.value.avatarUrl

            if (imageBytes != null && imageBytes.isNotEmpty()) {
                val uploadResult = repository.uploadAvatar(userId, imageBytes)
                uploadResult.onSuccess { url ->
                    newAvatarUrl = url
                }.onFailure { err ->
                    _profile.value = _profile.value.copy(isLoading = false)
                    onComplete(false, "Failed to upload profile photo: ${err.localizedMessage}")
                    return@launch
                }
            }

            val updatedDto = ProfileDto(
                id = userId,
                full_name = trimmedName,
                email = currentEmail,
                phone = trimmedPhone,
                username = trimmedUsername,
                avatar_url = newAvatarUrl
            )

            val updateResult = repository.upsertProfile(updatedDto)
            updateResult.onSuccess {
                _profile.value = _profile.value.copy(
                    name = trimmedName,
                    phone = trimmedPhone,
                    username = trimmedUsername,
                    avatarUrl = newAvatarUrl,
                    photoUri = photoUri,
                    photoBitmap = photoBitmap,
                    isLoading = false,
                    successMessage = "Profile updated successfully"
                )
                onComplete(true, "Profile updated successfully")
            }.onFailure { err ->
                _profile.value = _profile.value.copy(isLoading = false)
                onComplete(false, "Offline or server error: ${err.localizedMessage}")
            }
        }
    }

    fun clearMessages() {
        _profile.value = _profile.value.copy(errorMessage = null, successMessage = null)
    }
}
