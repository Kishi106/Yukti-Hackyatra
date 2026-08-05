package com.example.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.PotholeReport
import com.example.repository.ReportRepository
import com.example.supabase
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

sealed class ReportUiState {
    object Loading : ReportUiState()
    data class Success(val reports: List<PotholeReport>) : ReportUiState()
    data class Error(val message: String) : ReportUiState()
}

class ReportViewModel : ViewModel() {
    private val repository = ReportRepository.getInstance()

    val uiState: StateFlow<ReportUiState> = combine(
        repository.myReports,
        repository.myReportsLoading,
        repository.myReportsError
    ) { reports, isLoading, error ->
        when {
            isLoading && reports.isEmpty() -> ReportUiState.Loading
            error != null && reports.isEmpty() -> ReportUiState.Error(error)
            else -> ReportUiState.Success(reports)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = if (repository.myReports.value.isNotEmpty()) {
            ReportUiState.Success(repository.myReports.value)
        } else {
            ReportUiState.Loading
        }
    )

    val allPotholesState: StateFlow<ReportUiState> = combine(
        repository.allPotholes,
        repository.allPotholesLoading,
        repository.allPotholesError
    ) { reports, isLoading, error ->
        when {
            isLoading && reports.isEmpty() -> ReportUiState.Loading
            error != null && reports.isEmpty() -> ReportUiState.Error(error)
            else -> ReportUiState.Success(reports)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = if (repository.allPotholes.value.isNotEmpty()) {
            ReportUiState.Success(repository.allPotholes.value)
        } else {
            ReportUiState.Loading
        }
    )

    init {
        loadMyReports()
        loadAllPotholes()
    }

    fun loadAllPotholes() {
        viewModelScope.launch {
            try {
                repository.getAllPotholes()
            } catch (e: Exception) {
                Log.e("REPORT_SUBMIT", "loadAllPotholes error: ${e.localizedMessage}", e)
            }
        }
    }

    fun loadMyReports() {
        viewModelScope.launch {
            try {
                val userId = supabase.auth.currentSessionOrNull()?.user?.id
                if (userId.isNullOrBlank()) {
                    Log.e("REPORT_SUBMIT", "loadMyReports failed: User session is null")
                    return@launch
                }
                repository.getMyReports(userId)
            } catch (e: Exception) {
                Log.e("REPORT_SUBMIT", "loadMyReports error: ${e.localizedMessage}", e)
            }
        }
    }

    fun deleteReport(reportId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.deleteReport(reportId)
            onResult(success)
        }
    }

    fun updateReport(reportId: String, description: String, severity: String, photoUrl: String?, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.updateReport(reportId, description, severity, photoUrl)
            onResult(success)
        }
    }

    fun submitReport(
        report: PotholeReport,
        imageBytes: ByteArray?,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val user = supabase.auth.currentSessionOrNull()?.user
                val userId = user?.id
                if (userId.isNullOrBlank()) {
                    val authErr = "Authentication Error: No active user session. Please sign in."
                    Log.e("REPORT_SUBMIT", authErr)
                    onResult(false, authErr)
                    return@launch
                }
                val finalReport = report.copy(reporterId = userId)
                val result = repository.submitReport(finalReport, imageBytes)
                if (result.isSuccess) {
                    Log.d("REPORT_SUBMIT", "Report submitted successfully. Refreshing reports list.")
                    loadMyReports() // Refresh Home, My Reports, Live Map state
                    loadAllPotholes()
                    onResult(true, null)
                } else {
                    val ex = result.exceptionOrNull()
                    val errorDetail = ex?.localizedMessage ?: ex?.message ?: "Database insert failed"
                    Log.e("REPORT_SUBMIT", "Submission failed with error: $errorDetail", ex)
                    onResult(false, errorDetail)
                }
            } catch (e: Exception) {
                val fatalDetail = e.localizedMessage ?: e.message ?: "Unexpected error during report submission"
                Log.e("REPORT_SUBMIT", "Fatal submission exception: $fatalDetail", e)
                onResult(false, fatalDetail)
            }
        }
    }

    fun submitReport(
        report: PotholeReport,
        imageBytes: ByteArray?,
        onResult: (Boolean) -> Unit
    ) {
        submitReport(report, imageBytes) { success, _ -> onResult(success) }
    }
}
