package com.attentionmirror.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attentionmirror.data.AttentionRepository
import com.attentionmirror.domain.AttentionReceipt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val loading: Boolean = true,
    val hasUsageAccess: Boolean = false,
    val today: AttentionReceipt? = null,
    val week: AttentionReceipt? = null,
)

class AttentionViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AttentionRepository.create(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val hasAccess = repo.hasUsageAccess()
            if (hasAccess) repo.refresh()
            _state.value = UiState(
                loading = false,
                hasUsageAccess = hasAccess,
                today = repo.dailyReceipt(),
                week = repo.weeklyReceipt(),
            )
        }
    }
}
