package com.microsoft.research.karya.ui.onboarding.pin

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.microsoft.research.karya.R
import com.microsoft.research.karya.databinding.FragmentEnterPinCodeBinding
import com.microsoft.research.karya.ui.base.BaseFragment
import com.microsoft.research.karya.utils.extensions.disable
import com.microsoft.research.karya.utils.extensions.enable
import com.microsoft.research.karya.utils.extensions.gone
import com.microsoft.research.karya.utils.extensions.observe
import com.microsoft.research.karya.utils.extensions.viewBinding
import com.microsoft.research.karya.utils.extensions.viewLifecycle
import com.microsoft.research.karya.utils.extensions.visible
import com.zabaan.sdk.Zabaan
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectPinFragment : BaseFragment(R.layout.fragment_enter_pin_code) {

  private val binding by viewBinding(FragmentEnterPinCodeBinding::bind)
  private val viewModel by viewModels<SelectPinViewModel>()
  override val TAG: String = "SELECT_PIN_FRAGMENT"

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setupView()
    observeUi()
    observeEffects()
    // registrationActivity.current_assistant_audio = R.string.audio_age_prompt
  }

  override fun onResume() {
    super.onResume()
    Zabaan.getInstance().show(binding.root, viewLifecycle)
    Zabaan.getInstance().setCurrentState("IDLE")
    Zabaan.getInstance().setScreenName("AGE", autoPlay = true)
  }

  private fun setupView() {
    with(binding) {
      // appTb.setAssistantClickListener { assistant.playAssistantAudio(AssistantAudio.AGE_PROMPT) }

      pinEt.doAfterTextChanged { text ->
        if (text?.length == 6) {
          enablePinSubmitButton()
        } else {
          disablePinSubmitButton()
        }
      }

      submitPinIb.setOnClickListener { submitPin(pinEt.text.toString()) }
    }
  }

  private fun observeUi() {
    viewModel.selectAgeUiState.observe(viewLifecycleOwner.lifecycle, lifecycleScope) { state ->
      when (state) {
        is SelectPinUiState.Error -> showErrorUi(state.throwable.message!!)
          SelectPinUiState.Initial -> showInitialUi()
          SelectPinUiState.Loading -> showLoadingUi()
          SelectPinUiState.Success -> showSuccessUi()
      }
    }
  }

  private fun observeEffects() {
    viewModel.selectAgeEffects.observe(viewLifecycleOwner.lifecycle, lifecycleScope) { effect ->
      when (effect) {
          SelectPinEffects.Navigate -> navigateToDashboard()
      }
    }
  }

  private fun showInitialUi() {
    with(binding) {
      failToRegisterTv.gone()
      pinEt.text.clear()
      hideLoading()
      disablePinSubmitButton()
    }
  }

  private fun showLoadingUi() {
    with(binding) {
      failToRegisterTv.gone()
      showLoading()
      disablePinSubmitButton()
    }
  }

  private fun showSuccessUi() {
    with(binding) {
      failToRegisterTv.gone()
      hideLoading()
      enablePinSubmitButton()
    }
  }

  private fun showErrorUi(message: String) {
    with(binding) {
      failToRegisterTv.text = message
      failToRegisterTv.visible()
      hideLoading()
      enablePinSubmitButton()
    }
  }

  private fun disablePinSubmitButton() {
    binding.submitPinIb.disable()
  }

  private fun enablePinSubmitButton() {
    binding.submitPinIb.enable()
  }

  private fun submitPin(yob: String) {
    viewModel.updateWorkerProfile(yob)
  }

  private fun hideLoading() {
    with(binding) {
      loadingPb.gone()
      submitPinIb.visible()
    }
  }

  private fun showLoading() {
    with(binding) {
      loadingPb.visible()
      submitPinIb.gone()
    }
  }

  private fun navigateToDashboard() {
    findNavController().navigate(R.id.action_global_dashboardActivity4)
  }
}
