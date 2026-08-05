package com.example.repository

import android.util.Log
import com.example.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Serializable
data class ProfileDto(
    val id: String,
    val full_name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val username: String? = null,
    val avatar_url: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

class ProfileRepository {

    fun getCurrentUserId(): String? {
        return supabase.auth.currentSessionOrNull()?.user?.id
    }

    fun getCurrentUserEmail(): String? {
        return supabase.auth.currentSessionOrNull()?.user?.email
    }

    suspend fun getProfile(userId: String): Result<ProfileDto> = withContext(Dispatchers.IO) {
        try {
            val list = supabase.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeList<ProfileDto>()

            val profile = list.firstOrNull()
            if (profile != null) {
                Result.success(profile)
            } else {
                val email = getCurrentUserEmail() ?: ""
                val defaultProfile = ProfileDto(
                    id = userId,
                    full_name = email.substringBefore("@").replace(".", " ").capitalize(Locale.ROOT),
                    email = email,
                    phone = "",
                    username = email.substringBefore("@"),
                    avatar_url = null
                )
                upsertProfile(defaultProfile)
                Result.success(defaultProfile)
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "getProfile error: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun upsertProfile(profile: ProfileDto): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

            val payload = buildJsonObject {
                put("id", profile.id)
                put("full_name", profile.full_name ?: "")
                put("email", profile.email ?: "")
                put("phone", profile.phone ?: "")
                profile.username?.let { put("username", it) }
                profile.avatar_url?.let { put("avatar_url", it) }
                put("updated_at", nowIso)
            }

            supabase.postgrest["profiles"].upsert(payload)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "upsertProfile error: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = "avatar_${userId}_${System.currentTimeMillis()}.jpg"
            val bucket = supabase.storage["profile-photos"]

            bucket.upload(fileName, imageBytes) {
                upsert = true
            }

            val publicUrl = bucket.publicUrl(fileName)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "uploadAvatar error: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }
}
