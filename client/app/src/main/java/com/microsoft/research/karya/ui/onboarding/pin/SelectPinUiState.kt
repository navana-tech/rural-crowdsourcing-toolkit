package com.microsoft.research.karya.ui.onboarding.pin

sealed class SelectPinUiState {
  data class Error(val throwable: Throwable) : SelectPinUiState()
  object Initial : SelectPinUiState()
  object Loading : SelectPinUiState()
  object Success : SelectPinUiState()
}
