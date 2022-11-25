package com.microsoft.research.karya.data.remote.interceptors

import com.microsoft.research.karya.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class VersionInterceptor() : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    return chain.proceed(
      chain
        .request()
        .newBuilder()
        .addHeader("version-code", "${BuildConfig.VERSION_CODE}")
        .addHeader("version-name", BuildConfig.VERSION_NAME)
        .build()
    )

  }
}
