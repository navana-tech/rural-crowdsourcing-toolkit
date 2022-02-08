package com.microsoft.research.karya.ui.splashScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.microsoft.research.karya.data.manager.AuthManager
import com.microsoft.research.karya.data.model.karya.ng.WorkerRecord
import com.microsoft.research.karya.data.repo.WorkerRepository
import com.microsoft.research.karya.ui.Destination
import com.zabaan.common.ZabaanLanguages
import com.zabaan.sdk.Zabaan
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel
@Inject
constructor(
  private val authManager: AuthManager,
  private val workerRepository: WorkerRepository,
) : ViewModel() {

  private val _splashDestination = MutableSharedFlow<Destination>()
  val splashDestination = _splashDestination.asSharedFlow()

  private val _splashEffects = MutableSharedFlow<SplashEffects>()
  val splashEffects = _splashEffects.asSharedFlow()

  fun navigate() {
    viewModelScope.launch {
      val workers = getAllWorkers().size

      when (workers) {
        0 -> handleNewUser()
        1 -> handleSingleUser()
        else -> handleMultipleUsers()
      }
    }
  }

  private suspend fun getAllWorkers(): List<WorkerRecord> {
    return workerRepository.getAllWorkers()
  }

  private suspend fun getLoggedInWorker(): WorkerRecord {
    return authManager.fetchLoggedInWorker()
  }

  private suspend fun handleNewUser() {
    _splashDestination.emit(Destination.AccessCodeFlow)
  }

  private suspend fun handleSingleUser() {
    val worker = getLoggedInWorker()
    FirebaseCrashlytics.getInstance().setUserId(worker.accessCode)
    _splashEffects.emit(SplashEffects.UpdateLanguage(worker.language))
    if (worker.language.isNotEmpty())
      Zabaan.getInstance().setLanguage(ZabaanLanguages.getNavanaLanguage(worker.language))

    val isPinCodePresent = worker.profile?.has("pincode") ?: false
    val destination =
      when {
        !worker.isConsentProvided -> Destination.AccessCodeFlow
        worker.idToken.isNullOrEmpty() -> Destination.LoginFlow
        !isPinCodePresent -> Destination.MandatoryDataFlow
        else -> Destination.Dashboard
      }

    _splashDestination.emit(destination)
  }

  private suspend fun handleMultipleUsers() {
    // TODO: We do not support multiple user use-case for now
    // _splashDestination.emit(Destination.UserSelection)
    handleSingleUser()
  }
}
