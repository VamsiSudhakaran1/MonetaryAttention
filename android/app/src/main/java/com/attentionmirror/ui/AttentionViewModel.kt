package com.attentionmirror.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attentionmirror.data.AttentionRepository
import com.attentionmirror.data.WeekReport
import com.attentionmirror.domain.AttentionReceipt
import com.attentionmirror.domain.DefaultPlatforms
import com.attentionmirror.domain.DynamicMessage
import com.attentionmirror.domain.PlatformConfig
import com.attentionmirror.domain.UsageSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val loading: Boolean = true,
    val hasUsageAccess: Boolean = false,
    val hardTruthMode: Boolean = false,
    val quirkyMode: Boolean = false,
    val notificationHour: Int = 21,
    val notificationMinute: Int = 30,
    val today: AttentionReceipt? = null,
    val message: DynamicMessage? = null,
    val sessions: List<UsageSession> = emptyList(),
    val hourly: List<Long> = emptyList(),
    val week: WeekReport? = null,
    val monetizedPlatforms: List<PlatformConfig> = emptyList(),
    val adFreePackages: Set<String> = emptySet(),
)

class AttentionViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AttentionRepository.create(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val hasAccess = repo.hasUsageAccess()
            if (!hasAccess) {
                _state.value = UiState(loading = false, hasUsageAccess = false)
                return@launch
            }
            repo.refresh()
            val insights = repo.dayInsights()
            _state.value = UiState(
                loading = false,
                hasUsageAccess = true,
                hardTruthMode = repo.hardTruthMode,
                quirkyMode = repo.quirkyMode,
                notificationHour = repo.notificationHour,
                notificationMinute = repo.notificationMinute,
                today = insights.receipt,
                message = insights.message,
                sessions = insights.sessions,
                hourly = insights.hourlySeconds,
                week = repo.weekReport(),
                monetizedPlatforms = DefaultPlatforms.ALL.filter { it.monetized },
                adFreePackages = repo.adFreePackages(),
            )
        }
    }

    fun setHardTruthMode(enabled: Boolean) {
        repo.hardTruthMode = enabled
        // Rebuild so the message tone updates immediately.
        refresh()
    }

    fun setQuirkyMode(enabled: Boolean) {
        repo.quirkyMode = enabled
        refresh()
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        repo.setNotificationTime(hour, minute)
        refresh()
    }

    fun setAdFree(packageName: String, adFree: Boolean) {
        repo.setAdFree(packageName, adFree)
        refresh()
    }
}
