package com.microsoft.research.karya.ui.onboarding.age

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.microsoft.research.karya.data.manager.AuthManager
import com.microsoft.research.karya.data.repo.WorkerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SelectAgeViewModel
@Inject
constructor(
  private val authManager: AuthManager,
  private val workerRepository: WorkerRepository,
) : ViewModel() {

  private val _selectAgeUiState: MutableStateFlow<SelectAgeUiState> = MutableStateFlow(SelectAgeUiState.Initial)
  val selectAgeUiState = _selectAgeUiState.asStateFlow()

  private val _selectAgeEffects: MutableSharedFlow<SelectAgeEffects> = MutableSharedFlow()
  val selectAgeEffects = _selectAgeEffects.asSharedFlow()

  fun updateWorkerYOB(yob: String) {
    viewModelScope.launch {
      _selectAgeUiState.value = SelectAgeUiState.Loading

      val worker = authManager.fetchLoggedInWorker()
      checkNotNull(worker.idToken)
      checkNotNull(worker.gender)

      val newWorker = worker.copy(yob = yob)

      try {
        workerRepository.upsertWorker(newWorker)
        _selectAgeUiState.value = SelectAgeUiState.Success
        _selectAgeEffects.emit(SelectAgeEffects.Navigate)
      } catch (throwable: Throwable) {
        _selectAgeUiState.value = SelectAgeUiState.Error(throwable)
      }
    }
  }
}
