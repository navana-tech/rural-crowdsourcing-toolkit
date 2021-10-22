package com.microsoft.research.karya.data.manager

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.microsoft.research.karya.data.exceptions.NoWorkerException
import com.microsoft.research.karya.data.model.karya.ng.WorkerRecord
import com.microsoft.research.karya.data.repo.AuthRepository
import com.microsoft.research.karya.utils.PreferenceKeys
import com.microsoft.research.karya.utils.extensions.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

enum class AUTH_STATUS {
  LOGGED_IN,
  AUTHENTICATED,
  UNAUTHENTICATED,
  LOGGED_OUT
}

class AuthManager
@Inject
constructor(
  @ApplicationContext private val applicationContext: Context,
  private val authRepository: AuthRepository,
  private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private lateinit var activeWorkerId: String
  private val ioScope = CoroutineScope(defaultDispatcher)

  private val _currAuthStatus =
    MutableSharedFlow<AUTH_STATUS>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  val currAuthStatus = _currAuthStatus.asSharedFlow()

  suspend fun updateLoggedInWorker(workerId: String) {
    check(workerId.isNotEmpty()) { "accessCode cannot be null" }
    activeWorkerId = workerId
    setLoggedInWorkerID(workerId)
  }

  suspend fun fetchLoggedInWorker(): WorkerRecord {
    if (!this::activeWorkerId.isInitialized || activeWorkerId.isEmpty()) {
      activeWorkerId = getLoggedInWorkerId()
    }

    return authRepository.getWorkerById(activeWorkerId) ?: throw NoWorkerException()
  }

  suspend fun fetchLoggedInWorkerAccessCode(): String {
    val worker = fetchLoggedInWorker()
    return worker.accessCode
  }

  suspend fun startSession(worker: WorkerRecord) {
    authRepository.updateAfterAuth(worker)
    setAuthStatus(AUTH_STATUS.AUTHENTICATED)
  }

  fun expireSession() {
    setAuthStatus(AUTH_STATUS.UNAUTHENTICATED)
  }

  private suspend fun setLoggedInWorkerID(workerId: String) =
    withContext(defaultDispatcher) {
      val workerIdKey = stringPreferencesKey(PreferenceKeys.WORKER_ID)
      applicationContext.dataStore.edit { prefs -> prefs[workerIdKey] = workerId }
    }

  private suspend fun getLoggedInWorkerId(): String {
    if (!this::activeWorkerId.isInitialized || activeWorkerId.isEmpty()) {
      withContext(defaultDispatcher) {
        val workerIdKey = stringPreferencesKey(PreferenceKeys.WORKER_ID)
        val data = applicationContext.dataStore.data.first()
        activeWorkerId = data[workerIdKey] ?: throw NoWorkerException()
        resetAuthStatus()
      }
    }
    return activeWorkerId
  }

  private suspend fun resetAuthStatus() {
    try {
      val worker = fetchLoggedInWorker()
      if (worker.accessCode.isNotEmpty()) {
        if (!worker.idToken.isNullOrEmpty()) {
          setAuthStatus(AUTH_STATUS.AUTHENTICATED)
        } else {
          setAuthStatus(AUTH_STATUS.UNAUTHENTICATED)
        }
      } else {
        setAuthStatus(AUTH_STATUS.LOGGED_IN)
      }
    } catch (e: NoWorkerException) {
      setAuthStatus(AUTH_STATUS.LOGGED_OUT)
    }
  }

  private fun setAuthStatus(status: AUTH_STATUS) {
    _currAuthStatus.tryEmit(status)
  }
}
