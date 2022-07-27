package com.microsoft.research.karya.ui.onboarding.accesscode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.microsoft.research.karya.data.manager.AuthManager
import com.microsoft.research.karya.data.manager.BaseUrlManager
import com.microsoft.research.karya.data.model.karya.ng.WorkerRecord
import com.microsoft.research.karya.data.repo.WorkerRepository
import com.microsoft.research.karya.ui.onboarding.IncorrectAccessCodeException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class AccessCodeViewModel
@Inject
constructor(
  private val workerRepository: WorkerRepository,
  private val authManager: AuthManager,
  private val baseUrlManager: BaseUrlManager
) : ViewModel() {

  private val _accessCodeUiState: MutableStateFlow<AccessCodeUiState> = MutableStateFlow(AccessCodeUiState.Initial)
  val accessCodeUiState = _accessCodeUiState.asStateFlow()

  private val _accessCodeEffects: MutableSharedFlow<AccessCodeEffects> = MutableSharedFlow()
  val accessCodeEffects = _accessCodeEffects.asSharedFlow()

  fun setUrlAndCheckAccessCode(decodedURL: String, accessCode: String) {
    viewModelScope.launch {
      setURL(decodedURL)
      checkAccessCode(accessCode)
    }
  }

  private suspend fun checkAccessCode(accessCode: String) {
    workerRepository
      .verifyAccessCode(accessCode)
      .onStart { _accessCodeUiState.value = AccessCodeUiState.Loading }
      .onEach { worker ->
        createWorker(accessCode, worker)
        authManager.updateLoggedInWorker(worker.id)
        _accessCodeUiState.value = AccessCodeUiState.Success(worker.language)
        _accessCodeEffects.emit(AccessCodeEffects.Navigate)
      }
      .catch {
        _accessCodeUiState.value =
          AccessCodeUiState.Error(
            IncorrectAccessCodeException("Cannot verify this access code, please try again later.")
          )
      }.collect()
  }

  private suspend fun createWorker(accessCode: String, workerRecord: WorkerRecord) =
    withContext(Dispatchers.IO) {
      val dbWorker = workerRecord.copy(accessCode = accessCode)
      workerRepository.upsertWorker(dbWorker)
    }

  private suspend fun setURL(decodedURL: String) {
    baseUrlManager.updateBaseUrl(decodedURL)
  }
}
