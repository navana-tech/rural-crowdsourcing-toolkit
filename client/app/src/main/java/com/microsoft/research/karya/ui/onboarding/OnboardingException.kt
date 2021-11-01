package com.microsoft.research.karya.ui.onboarding

sealed class OnboardingException
@JvmOverloads
constructor(
  message: String? = null,
  cause: Throwable? = null,
) : Exception(message, cause)

class IncorrectAccessCodeException
@JvmOverloads
constructor(
  message: String? = null,
  cause: Throwable? = null,
) : OnboardingException()

class IncorrectPhoneNumberException
@JvmOverloads
constructor(
  message: String? = null,
  cause: Throwable? = null,
) : OnboardingException(message, cause)
