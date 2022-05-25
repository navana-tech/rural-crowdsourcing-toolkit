package com.microsoft.research.karya.ui.payment.verification

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.microsoft.research.karya.R
import com.microsoft.research.karya.databinding.FragmentPaymentVerificationBinding
import com.microsoft.research.karya.utils.extensions.gone
import com.microsoft.research.karya.utils.extensions.observe
import com.microsoft.research.karya.utils.extensions.viewBinding
import com.microsoft.research.karya.utils.extensions.viewLifecycle
import com.microsoft.research.karya.utils.extensions.viewLifecycleScope
import com.microsoft.research.karya.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class PaymentVerificationFragment : Fragment(R.layout.fragment_payment_verification) {
    private val binding by viewBinding(FragmentPaymentVerificationBinding::bind)
    private val viewModel by viewModels<PaymentVerificationViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        viewModel.checkStatus()
    }

    private fun setupListeners() {
        binding.successBtn.setOnClickListener {
            confirmStatus(true)
        }

        binding.failureBtn.setOnClickListener {
            confirmStatus(false)
        }

        viewModel.uiStateFlow.observe(viewLifecycle, viewLifecycleScope) { paymentModel ->
            Log.d("VALUE_EMITTED", paymentModel.toString())
            render(paymentModel)
        }

        viewModel.navigationFlow.onEach { navigation ->
            when (navigation) {
                PaymentVerificationNavigation.DASHBOARD -> navigateToDashboard()
                PaymentVerificationNavigation.FAILURE -> navigateToFailure()
            }
        }.launchIn(viewLifecycleScope)
    }

    private fun render(paymentVerificationModel: PaymentVerificationModel) {
        if (paymentVerificationModel.isLoading) {
            showNetworkRequest(paymentVerificationModel)
        } else {
            when (paymentVerificationModel.state) {
                PaymentVerificationState.UNKNOWN -> showNetworkRequest(paymentVerificationModel)
                PaymentVerificationState.REQUEST_PROCESSING -> showRequestProcessing(paymentVerificationModel)
                PaymentVerificationState.REQUEST_PROCESSED -> showRequestProcessed(paymentVerificationModel)
                PaymentVerificationState.FEEDBACK_RECEIVED -> showFeedbackProcessing(paymentVerificationModel)
            }
/*
            if (paymentVerificationModel.state) {
                with(binding) {
                    failureBtn.visible()
                    successBtn.visible()
                    title.visible()
                    description.text = getString(R.string.verification_request_processed)
                    description.visible()
                    progressLL.gone()
                }
            } else {
                with(binding) {
                    failureBtn.gone()
                    successBtn.gone()
                    title.visible()
                    description.text = getString(R.string.verification_request_processing)
                    description.visible()
                    progressLL.gone()
                }
            }
*/
        }
    }

    private fun showNetworkRequest(paymentVerificationModel: PaymentVerificationModel) {
        with(binding) {
            failureBtn.gone()
            successBtn.gone()
            title.gone()
            description.gone()
            progressBar.visible()
            progressText.setText(R.string.checking_account_status)
            progressImage.setImageResource(R.drawable.ic_wait)
            progressLL.visible()
        }
    }

    private fun showRequestProcessing(paymentVerificationModel: PaymentVerificationModel) {
        with(binding) {
            failureBtn.gone()
            successBtn.gone()
            title.gone()
            description.gone()
            progressBar.gone()
            progressText.setText(R.string.processing_account_details)
            progressImage.setImageResource(R.drawable.ic_processing)
            progressLL.visible()
        }
    }

    private fun showRequestProcessed(paymentVerificationModel: PaymentVerificationModel) {
        with(binding) {
            failureBtn.visible()
            successBtn.visible()
            title.visible()
            description.text = getString(R.string.verification_request_processed, paymentVerificationModel.utr)
            description.visible()
            progressLL.gone()
        }
    }

    private fun showFeedbackProcessing(paymentVerificationModel: PaymentVerificationModel) {
        with(binding) {
            failureBtn.gone()
            successBtn.gone()
            title.gone()
            description.gone()
            progressBar.gone()
            progressText.setText(R.string.account_feedback_received)
            progressImage.setImageResource(R.drawable.ic_processing)
            progressLL.visible()
        }
    }

    private fun navigateToDashboard() {
         findNavController().navigate(R.id.action_paymentVerificationFragment_to_paymentDashboardFragment)
    }

    private fun navigateToFailure() {
        findNavController().navigate(R.id.action_global_paymentFailureFragment)
    }

    private fun confirmStatus(confirm: Boolean) {
        viewModel.verifyAccount(confirm)
    }

    companion object {
        fun newInstance() = PaymentVerificationFragment()
    }
}
