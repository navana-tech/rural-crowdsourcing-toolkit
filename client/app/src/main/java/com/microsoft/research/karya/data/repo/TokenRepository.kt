package com.microsoft.research.karya.data.repo

import com.microsoft.research.karya.data.local.ng.WorkerDao
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TokenRepository @Inject constructor(private val workerDao: WorkerDao) {

  suspend fun renewIdToken(id: String, newIdToken: String) =
    withContext(Dispatchers.IO) {
      val worker = workerDao.getById(id)
      val updatedWorker = worker!!.copy(idToken = newIdToken)
      workerDao.upsert(updatedWorker)
    }
}
