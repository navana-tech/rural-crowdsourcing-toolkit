package com.microsoft.research.karya.ui.payment.verification

enum class PaymentVerificationState {
    UNKNOWN,
    REQUEST_PROCESSING,
    REQUEST_PROCESSED,
    FEEDBACK_RECEIVED,
    ;
}

data class PaymentVerificationModel(
    val isLoading: Boolean,
    val state: PaymentVerificationState,
    val utr: String = "",
    val errorMessage: String = "",
)
