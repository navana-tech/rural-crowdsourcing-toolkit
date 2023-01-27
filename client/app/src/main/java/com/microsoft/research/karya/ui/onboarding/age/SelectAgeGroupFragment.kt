package com.microsoft.research.karya.ui.onboarding.age

import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.microsoft.research.karya.R
import com.microsoft.research.karya.databinding.FragmentSelectAgeGroupBinding
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
import kotlinx.android.synthetic.main.fragment_select_age_group.age_group_1_rb
import kotlinx.android.synthetic.main.fragment_select_age_group.age_group_2_rb
import kotlinx.android.synthetic.main.fragment_select_age_group.age_group_3_rb

@AndroidEntryPoint
class SelectAgeGroupFragment : BaseFragment(R.layout.fragment_select_age_group) {

  private val binding by viewBinding(FragmentSelectAgeGroupBinding::bind)
  private val viewModel by viewModels<SelectAgeViewModel>()
  override val TAG: String = "SELECT_AGE_GROUP_FRAGMENT"

  private var currentAgeGroup = R.id.age_group_1_rb

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

      println("Current ID $currentAgeGroup")
      resetViewState()


      ageGroup1Rb.setOnClickListener {
        currentAgeGroup = R.id.age_group_1_rb
        resetViewState()
      }
      ageGroup2Rb.setOnClickListener {
        currentAgeGroup = R.id.age_group_2_rb
        resetViewState()
      }
      ageGroup3Rb.setOnClickListener {
        currentAgeGroup = R.id.age_group_3_rb
        resetViewState()
      }

      ageEt.doAfterTextChanged { text ->
        if (text?.length == 4) {
          enableAgeGroupSubmitButton()
        } else {
          disableAgeGroupSubmitButton()
        }
      }

      submitAgeGroupIb.setOnClickListener {

        val deviceId = Settings.Secure.getString(
          it.context.getContentResolver(),
          Settings.Secure.ANDROID_ID
        )
        when (currentAgeGroup) {
          R.id.age_group_1_rb -> submitYOB(ageEt.text.toString(), "18-25", deviceId)
          R.id.age_group_2_rb -> submitYOB(ageEt.text.toString(), "26-55", deviceId)
          R.id.age_group_3_rb -> submitYOB(ageEt.text.toString(), "56+", deviceId)
        }
      }
    }
  }

  private fun resetViewState() {
    println("Current ID $currentAgeGroup")
    with(binding) {
      rg.clearCheck()
      when (currentAgeGroup) {
        R.id.age_group_1_rb -> {
          age_group_1_rb.isChecked = true
          age_group_2_rb.isChecked = false
          age_group_3_rb.isChecked = false
        }

        R.id.age_group_2_rb -> {
          age_group_1_rb.isChecked = false
          age_group_2_rb.isChecked = true
          age_group_3_rb.isChecked = false
        }

        else -> {
          age_group_1_rb.isChecked = false
          age_group_2_rb.isChecked = false
          age_group_3_rb.isChecked = true
        }
      }
    }
  }

  private fun observeUi() {
    viewModel.selectAgeUiState.observe(viewLifecycleOwner.lifecycle, lifecycleScope) { state ->
      when (state) {
        is SelectAgeUiState.Error -> showErrorUi(state.throwable.message!!)
        SelectAgeUiState.Initial -> showInitialUi()
        SelectAgeUiState.Loading -> showLoadingUi()
        SelectAgeUiState.Success -> showSuccessUi()
      }
    }
  }

  private fun observeEffects() {
    viewModel.selectAgeEffects.observe(viewLifecycleOwner.lifecycle, lifecycleScope) { effect ->
      when (effect) {
        SelectAgeEffects.Navigate -> navigateToDashboard()
      }
    }
  }

  private fun showInitialUi() {
    with(binding) {
      failToRegisterTv.gone()
      ageEt.text.clear()
      hideLoading()
      disableAgeGroupSubmitButton()
    }
  }

  private fun showLoadingUi() {
    with(binding) {
      failToRegisterTv.gone()
      showLoading()
      disableAgeGroupSubmitButton()
    }
  }

  private fun showSuccessUi() {
    with(binding) {
      failToRegisterTv.gone()
      hideLoading()
      enableAgeGroupSubmitButton()
    }
  }

  private fun showErrorUi(message: String) {
    with(binding) {
      failToRegisterTv.text = message
      failToRegisterTv.visible()
      hideLoading()
      enableAgeGroupSubmitButton()
    }
  }

  private fun disableAgeGroupSubmitButton() {
    binding.submitAgeGroupIb.disable()
  }

  private fun enableAgeGroupSubmitButton() {
    binding.submitAgeGroupIb.enable()
  }

  private fun submitYOB(yob: String, group: String, deviceId: String) {
    println("Submit from fragment $yob - $group")
    viewModel.updateWorkerYOB(yob, group, deviceId)
  }

  private fun hideLoading() {
    with(binding) {
      loadingPb.gone()
      submitAgeGroupIb.visible()
    }
  }

  private fun showLoading() {
    with(binding) {
      loadingPb.visible()
      submitAgeGroupIb.gone()
    }
  }

  private fun navigateToDashboard() {
    findNavController().navigate(R.id.action_selectAgeGroupFragment2_to_selectPinFragment)
  }
}
