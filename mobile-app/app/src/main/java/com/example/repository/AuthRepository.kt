package com.example.repository

import com.example.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val full_name: String,
    val email: String,
    val phone: String
)

class AuthRepository {
    
    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(name: String, email: String, phone: String, password: String): Result<Unit> {
        return try {
            val user = supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            // Add user profile
            val session = supabase.auth.currentSessionOrNull()
            val userId = session?.user?.id ?: user?.id
            
            if (userId != null) {
                val profile = Profile(
                    id = userId,
                    full_name = name,
                    email = email,
                    phone = phone
                )
                supabase.postgrest["profiles"].insert(profile)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
    return try {
        supabase.auth.resetPasswordForEmail(
            email = email,
            redirectUrl = "spotv://reset-password"
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
    
    
    suspend fun updatePassword(password: String): Result<Unit> {
        return try {
            supabase.auth.updateUser {
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun hasSession(): Boolean {
        return supabase.auth.currentSessionOrNull() != null
    }

    suspend fun checkSession(): Boolean {
        supabase.auth.awaitInitialization()
        return supabase.auth.currentSessionOrNull() != null
    }
}
