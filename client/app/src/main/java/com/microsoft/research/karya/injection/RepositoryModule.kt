package com.microsoft.research.karya.injection

import com.microsoft.research.karya.data.local.daos.KaryaFileDao
import com.microsoft.research.karya.data.local.daos.MicroTaskDao
import com.microsoft.research.karya.data.local.daos.PaymentAccountDao
import com.microsoft.research.karya.data.local.daos.WorkerDao
import com.microsoft.research.karya.data.local.daosExtra.MicrotaskDaoExtra
import com.microsoft.research.karya.data.local.ng.WorkerDao
import com.microsoft.research.karya.data.repo.AuthRepository
import com.microsoft.research.karya.data.repo.KaryaFileRepository
import com.microsoft.research.karya.data.repo.LanguageRepository
import com.microsoft.research.karya.data.repo.MicroTaskRepository
import com.microsoft.research.karya.data.repo.WorkerRepository
import com.microsoft.research.karya.data.service.KaryaFileAPI
import com.microsoft.research.karya.data.service.LanguageAPI
import com.microsoft.research.karya.data.service.PaymentAPI
import com.microsoft.research.karya.data.service.WorkerAPI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

  @Provides
  @Singleton
  fun provideLanguageRepository(languageAPI: LanguageAPI): LanguageRepository {
    return LanguageRepository(languageAPI)
  }

  @Provides
  @Singleton
  fun provideMicroTaskRepository(
    microTaskDao: MicroTaskDao,
    microtaskDaoExtra: MicrotaskDaoExtra
  ): MicroTaskRepository {
    return MicroTaskRepository(microTaskDao, microtaskDaoExtra)
  }

  @Provides
  @Singleton
  fun provideWorkerRepository(workerAPI: WorkerAPI, workerDao: WorkerDao): WorkerRepository {
    return WorkerRepository(workerAPI, workerDao)
  }

  @Provides
  @Singleton
  fun provideKaryaFileRepository(karyaFileAPI: KaryaFileAPI, karyaFileDao: KaryaFileDao): KaryaFileRepository {
    return KaryaFileRepository(karyaFileAPI, karyaFileDao)
  }

  @Provides
  @Singleton
  fun provideAuthRepository(workerDao: WorkerDao): AuthRepository {
    return AuthRepository(workerDao)
  }

  @Provides
  @Singleton
  fun providesPaymentRepository(paymentAPI: PaymentAPI, paymentAccountDao: PaymentAccountDao): PaymentRepository {
      return PaymentRepository(paymentAPI, paymentAccountDao)
  }
}
