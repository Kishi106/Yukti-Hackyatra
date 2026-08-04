package com.example.viewmodels

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val name: String = "Balaji Tekupudi",
    val phone: String = "+91 98765 43210",
    val email: String = "tekupudibalaji@gmail.com",
    val photoUri: Uri? = null,
    val photoBitmap: Bitmap? = null
)

class ProfileViewModel : ViewModel() {
    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    fun updateProfile(name: String, phone: String, email: String, photoUri: Uri? = null, photoBitmap: Bitmap? = null) {
        _profile.value = _profile.value.copy(
            name = name.ifBlank { _profile.value.name },
            phone = phone.ifBlank { _profile.value.phone },
            email = email.ifBlank { _profile.value.email },
            photoUri = photoUri ?: _profile.value.photoUri,
            photoBitmap = photoBitmap ?: _profile.value.photoBitmap
        )
    }

    fun updatePhoto(uri: Uri?, bitmap: Bitmap?) {
        _profile.value = _profile.value.copy(
            photoUri = uri,
            photoBitmap = bitmap
        )
    }
}
