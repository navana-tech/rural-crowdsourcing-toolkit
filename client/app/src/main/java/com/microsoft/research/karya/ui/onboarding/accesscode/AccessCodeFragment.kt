package com.microsoft.research.karya.ui.onboarding.accesscode

import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.microsoft.research.karya.R
import com.microsoft.research.karya.databinding.FragmentAccessCodeBinding
import com.microsoft.research.karya.ui.MainActivity
import com.microsoft.research.karya.utils.SeparatorTextWatcher
import com.microsoft.research.karya.utils.WorkerLanguage
import com.microsoft.research.karya.utils.extensions.*
import com.zabaan.common.ZabaanLanguages
import com.zabaan.sdk.Zabaan
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccessCodeFragment : Fragment(R.layout.fragment_access_code) {
  private val binding by viewBinding(FragmentAccessCodeBinding::bind)
  private val viewModel by viewModels<AccessCodeViewModel>()

  private val creationCodeLength = 16
  private val creationCodeEtMax = creationCodeLength + (creationCodeLength - 1) / 4

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupViews()
    observeUi()
    observeEffects()

    Zabaan.getInstance().show(binding.rootCl, viewLifecycle)
    Zabaan.getInstance().setCurrentState("IDLE")
    Zabaan.getInstance().setScreenName("ACCESS_CODE", autoPlay = false)

    binding.volumeDialog.isVisible = isVolumeLowerThan(MIN_VOLUME)
  }

  override fun onResume() {
    super.onResume()
  }

  private fun setupViews() {
    with(binding) {
      creationCodeEt.addTextChangedListener(
        object : SeparatorTextWatcher('-', 4) {
          override fun onAfterTextChanged(text: String, position: Int) {
            creationCodeEt.run {
              setText(text)
              setSelection(position)
            }

            if (creationCodeEt.length() == creationCodeEtMax) {
              enableButton()
              creationCodeEt.setSelection(creationCodeEtMax)
            } else {
              disableButton()
            }
          }
        }
      )

      submitAccessCodeBtn.setOnClickListener {
        val accessCode = binding.creationCodeEt.text.toString().replace("-", "")
        val decodedURL = AccessCodeDecoder.decodeURL(requireContext(), accessCode)

        viewModel.setUrlAndCheckAccessCode(decodedURL, accessCode)
      }

      requestSoftKeyFocus(creationCodeEt)
    }
  }

  private fun observeUi() {
    viewModel.accessCodeUiState.observe(viewLifecycle, viewLifecycleScope) { state ->
      when (state) {
        is AccessCodeUiState.Success -> showSuccessUi(state.languageCode)
        is AccessCodeUiState.Error -> showErrorUi(state.throwable.message ?: "Incorrect Access Code")
        AccessCodeUiState.Initial -> showInitialUi()
        AccessCodeUiState.Loading -> showLoadingUi()
      }
    }
  }

  private fun observeEffects() {
    viewModel.accessCodeEffects.observe(viewLifecycle, viewLifecycleScope) { effect ->
      when (effect) {
        AccessCodeEffects.Navigate -> navigateToConsentFormFragment()
      }
    }
  }

  private fun navigateToConsentFormFragment() {
    findNavController().navigate(R.id.action_accessCodeFragment2_to_fileDownloadFragment2)
  }

  private fun showSuccessUi(languageCode: String) {
    Zabaan.getInstance().setLanguage(ZabaanLanguages.getNavanaLanguage(languageCode.lowercase()))
    WorkerLanguage.language = languageCode.lowercase()
    updateActivityLanguage(languageCode)

    hideLoading()
    hideError()
    enableButton()
  }

  private fun showErrorUi(error: String) {
    showError(error)
    hideLoading()
    enableButton()
    requestSoftKeyFocus(binding.creationCodeEt)
  }

  private fun showInitialUi() {
    hideLoading()
    disableButton()
    hideError()
    binding.creationCodeEt.text.clear()
  }

  private fun showLoadingUi() {
    showLoading()
    disableButton()
  }

  private fun showError(message: String) {
    with(binding) { creationCodeErrorTv.text = message }
  }

  private fun hideError() {
    with(binding) { creationCodeErrorTv.text = "" }
  }

  private fun showLoading() {
    with(binding) {
      loadingPb.visible()
      submitAccessCodeBtn.gone()
      creationCodeEt.isEnabled = false
    }
  }

  private fun hideLoading() {
    with(binding) {
      loadingPb.gone()
      submitAccessCodeBtn.visible()
      creationCodeEt.isEnabled = true
    }
  }

  private fun disableButton() {
    binding.submitAccessCodeBtn.disable()
  }

  private fun enableButton() {
    binding.submitAccessCodeBtn.enable()
  }

  private fun updateActivityLanguage(language: String) {
    (requireActivity() as MainActivity).setActivityLocale(language)
  }

  private fun isVolumeLowerThan(threshold: Float): Boolean {
    Log.d("KaryaDialog", "isVolumeLower")
    val audioManager = requireContext().getSystemService<AudioManager>() ?: return false
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
    Log.d("KaryaDialog", "currentVolume: $currentVolume")
    return (currentVolume / maxVolume < threshold)
  }

  companion object {
    const val MIN_VOLUME = 0.3f
  }
}
