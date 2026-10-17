package com.ragdroid.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ragdroid.domain.usecase.GetIngestionStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getIngestionStatusUseCase: GetIngestionStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        getIngestionStatusUseCase.observeAll()
            .onEach { documents ->
                _uiState.update {
                    it.copy(
                        documentCount = documents.size,
                        recentDocuments = documents.take(5),
                        allDocuments = documents,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
