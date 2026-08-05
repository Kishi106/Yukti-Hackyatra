package com.example.repository

import android.util.Log
import com.example.models.PotholeReport
import com.example.supabase
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
import java.util.UUID

import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Serializable
data class PotholeUpdate(
    val description: String,
    val severity: String,
    val photo_url: String? = null
)

class ReportRepository private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: ReportRepository? = null

        fun getInstance(): ReportRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReportRepository().also { INSTANCE = it }
            }
        }
    }

    private val _myReports = MutableStateFlow<List<PotholeReport>>(emptyList())
    val myReports: StateFlow<List<PotholeReport>> = _myReports.asStateFlow()

    private val _allPotholes = MutableStateFlow<List<PotholeReport>>(emptyList())
    val allPotholes: StateFlow<List<PotholeReport>> = _allPotholes.asStateFlow()

    private val _myReportsLoading = MutableStateFlow(false)
    val myReportsLoading: StateFlow<Boolean> = _myReportsLoading.asStateFlow()

    private val _allPotholesLoading = MutableStateFlow(false)
    val allPotholesLoading: StateFlow<Boolean> = _allPotholesLoading.asStateFlow()

    private val _myReportsError = MutableStateFlow<String?>(null)
    val myReportsError: StateFlow<String?> = _myReportsError.asStateFlow()

    private val _allPotholesError = MutableStateFlow<String?>(null)
    val allPotholesError: StateFlow<String?> = _allPotholesError.asStateFlow()

    suspend fun getMyReports(userId: String): List<PotholeReport> = withContext(Dispatchers.IO) {
        if (_myReports.value.isEmpty()) {
            _myReportsLoading.value = true
        }
        _myReportsError.value = null
        try {
            val reports = supabase.postgrest["potholes"]
                .select {
                    filter {
                        eq("reporter_id", userId)
                    }
                }
                .decodeList<PotholeReport>()
            _myReports.value = reports
            _myReportsLoading.value = false
            reports
        } catch (e: Exception) {
            Log.e("REPORT_SUBMIT", "getMyReports error: ${e.localizedMessage}", e)
            _myReportsError.value = e.localizedMessage ?: "Failed to load my reports"
            _myReportsLoading.value = false
            _myReports.value
        }
    }

    suspend fun getAllPotholes(): List<PotholeReport> = withContext(Dispatchers.IO) {
        if (_allPotholes.value.isEmpty()) {
            _allPotholesLoading.value = true
        }
        _allPotholesError.value = null
        try {
            val reports = supabase.postgrest["potholes"]
                .select()
                .decodeList<PotholeReport>()
            _allPotholes.value = reports
            _allPotholesLoading.value = false
            reports
        } catch (e: Exception) {
            Log.e("REPORT_SUBMIT", "getAllPotholes error: ${e.localizedMessage}", e)
            _allPotholesError.value = e.localizedMessage ?: "Failed to load all potholes"
            _allPotholesLoading.value = false
            _allPotholes.value
        }
    }

    suspend fun submitReport(
        report: PotholeReport,
        imageBytes: ByteArray? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var photoUrl: String? = null

            if (imageBytes != null && imageBytes.isNotEmpty()) {
                try {
                    val fileName =
                        "pothole_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
                    val bucket = supabase.storage["pothole-photos"]
                    bucket.upload(fileName, imageBytes) { upsert = true }
                    photoUrl = bucket.publicUrl(fileName)
                } catch (e: Exception) {
                    Log.e("REPORT_SUBMIT", "Photo upload failed", e)
                }
            }

            val nowIso = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                Locale.US
            ).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

            val payload = buildJsonObject {
                report.reporterId?.let { put("reporter_id", it) }
                put("lat", report.lat)
                put("lng", report.lng)

                val severityNorm = when (report.severity.trim().lowercase()) {
                    "low" -> "low"
                    "medium" -> "medium"
                    "high" -> "high"
                    else -> "medium"
                }
                put("severity", severityNorm)

                val status = when (report.status.trim().lowercase()) {
                    "new" -> "new"
                    "in_progress", "in progress" -> "in_progress"
                    "fixed", "repaired" -> "fixed"
                    else -> "new"
                }
                put("status", status)

                val source = when (report.source.trim().lowercase()) {
                    "auto", "sensor" -> "auto"
                    else -> "citizen"
                }
                put("source", source)

                report.description?.let { if (it.isNotBlank()) put("description", it) }
                put("created_at", report.createdAt ?: nowIso)
                put("confidence_score", report.confidenceScore.toInt())
                put("user_confirmed", report.userConfirmed ?: true)
                put("ward", report.ward ?: "Ward 22")
                put("ward_no", report.wardNo?.toIntOrNull() ?: 22)
                put("location", report.location ?: "${report.lat}, ${report.lng}")
                put("address", report.address ?: "Unknown Address")
                report.zone?.toIntOrNull()?.let { put("zone", it) }
                report.potholeCode?.let { put("pothole_code", it) }
                photoUrl?.let { put("photo_url", it) }
            }

            supabase.postgrest["potholes"].insert(payload)

            // Instantly refresh shared state
            val userId = supabase.auth.currentSessionOrNull()?.user?.id
            if (!userId.isNullOrBlank()) {
                getMyReports(userId)
            }
            getAllPotholes()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("REPORT_SUBMIT", "Submission Exception", e)
            Result.failure(e)
        }
    }

    suspend fun deleteReport(reportId: String): Boolean = withContext(Dispatchers.IO) {
        // Optimistic removal: update shared StateFlows immediately so all UI screens react instantly
        _myReports.value = _myReports.value.filter { it.id != reportId }
        _allPotholes.value = _allPotholes.value.filter { it.id != reportId }

        try {
            supabase.postgrest["potholes"].delete {
                filter {
                    eq("id", reportId)
                }
            }
            val userId = supabase.auth.currentSessionOrNull()?.user?.id
            if (!userId.isNullOrBlank()) {
                getMyReports(userId)
            }
            getAllPotholes()
            true
        } catch (e: Exception) {
            Log.e("REPORT_SUBMIT", "Delete report error: ${e.localizedMessage}", e)
            val userId = supabase.auth.currentSessionOrNull()?.user?.id
            if (!userId.isNullOrBlank()) {
                getMyReports(userId)
            }
            getAllPotholes()
            false
        }
    }

    suspend fun updateReport(reportId: String, description: String, severity: String, photoUrl: String?): Boolean = withContext(Dispatchers.IO) {
        // Optimistic update for immediate UI reaction
        _myReports.value = _myReports.value.map {
            if (it.id == reportId) it.copy(description = description, severity = severity, photoUrl = photoUrl ?: it.photoUrl)
            else it
        }
        _allPotholes.value = _allPotholes.value.map {
            if (it.id == reportId) it.copy(description = description, severity = severity, photoUrl = photoUrl ?: it.photoUrl)
            else it
        }

        try {
            val update = PotholeUpdate(description, severity, photoUrl)
            supabase.postgrest["potholes"].update(update) {
                filter {
                    eq("id", reportId)
                }
            }
            val userId = supabase.auth.currentSessionOrNull()?.user?.id
            if (!userId.isNullOrBlank()) {
                getMyReports(userId)
            }
            getAllPotholes()
            true
        } catch (e: Exception) {
            Log.e("REPORT_SUBMIT", "Update report error: ${e.localizedMessage}", e)
            false
        }
    }
}
