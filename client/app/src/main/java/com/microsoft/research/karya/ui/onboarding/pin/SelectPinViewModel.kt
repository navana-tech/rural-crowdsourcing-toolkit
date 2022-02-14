package com.microsoft.research.karya.ui.onboarding.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.microsoft.research.karya.data.manager.AuthManager
import com.microsoft.research.karya.data.remote.request.RegisterOrUpdateWorkerRequest
import com.microsoft.research.karya.data.repo.WorkerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class SelectPinViewModel
@Inject
constructor(
  private val authManager: AuthManager,
  private val workerRepository: WorkerRepository,
) : ViewModel() {

  private val _selectPinUiState: MutableStateFlow<SelectPinUiState> = MutableStateFlow(SelectPinUiState.Initial)
  val selectAgeUiState = _selectPinUiState.asStateFlow()

  private val _selectPinEffects: MutableSharedFlow<SelectPinEffects> = MutableSharedFlow()
  val selectAgeEffects = _selectPinEffects.asSharedFlow()

  fun updateWorkerProfile(pin: String) {
    viewModelScope.launch {
      _selectPinUiState.value = SelectPinUiState.Loading

      val worker = authManager.fetchLoggedInWorker()
      checkNotNull(worker.idToken)
      checkNotNull(worker.gender)
      checkNotNull(worker.yob)

      val profile = JsonObject()
      profile.addProperty("pincode", pin)
      val registerOrUpdateWorkerRequest = RegisterOrUpdateWorkerRequest(worker.yob, worker.gender, profile)

      workerRepository
        .updateWorker(worker.idToken, worker.accessCode, registerOrUpdateWorkerRequest)
        .onEach { workerRecord ->
          workerRepository.upsertWorker(worker.copy(profile = workerRecord.profile))
          _selectPinUiState.value = SelectPinUiState.Success
          _selectPinEffects.emit(SelectPinEffects.Navigate)
        }
        .catch { e -> _selectPinUiState.value = SelectPinUiState.Error(e) }
        .launchIn(viewModelScope)
    }
  }
}
