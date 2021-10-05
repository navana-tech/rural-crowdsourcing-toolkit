package com.microsoft.research.karya.ui.onboarding.profile

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.microsoft.research.karya.R
import com.microsoft.research.karya.data.local.enum.AssistantAudio
import com.microsoft.research.karya.databinding.FragmentProfilePictureBinding
import com.microsoft.research.karya.ui.Destination
import com.microsoft.research.karya.ui.base.BaseFragment
import com.microsoft.research.karya.ui.base.SessionFragment
import com.microsoft.research.karya.utils.extensions.disable
import com.microsoft.research.karya.utils.extensions.enable
import com.microsoft.research.karya.utils.extensions.dataStore
import com.microsoft.research.karya.utils.extensions.doOnlyOnce
import com.microsoft.research.karya.utils.extensions.gone
import com.microsoft.research.karya.utils.extensions.observe
import com.microsoft.research.karya.utils.extensions.viewBinding
import com.microsoft.research.karya.utils.extensions.viewLifecycle
import com.microsoft.research.karya.utils.extensions.viewLifecycleScope
import com.microsoft.research.karya.utils.extensions.visible
import com.zabaan.sdk.AssistantStateListener
import com.zabaan.sdk.Zabaan
import com.zabaan.sdk.internal.interaction.StateInteractionRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : SessionFragment(R.layout.fragment_profile_picture), AssistantStateListener {

  private val binding by viewBinding(FragmentProfilePictureBinding::bind)
  private val viewModel by viewModels<ProfileViewModel>()
  override val TAG: String = "PROFILE_FRAGMENT"

  private val selectPicture =
    registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
      if (bitmap == null) return@registerForActivityResult

      viewModel.saveProfileImage(bitmap)
    }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setupView()
    observeUi()
    observeEffects()
  }

    override fun onResume() {
        super.onResume()
        Zabaan.getInstance().apply {
            show(binding.root, viewLifecycle)
            setAssistantStateListener(this@ProfileFragment)
            setCurrentState("IDLE")
            setScreenName("PROFILE_PICTURE")
        }
    }

  private fun setupView() {
    with(binding) {
      mainProfilePictureIv.setOnClickListener { selectPicture.launch(null) }

      profilePictureNextBtn.setOnClickListener { viewModel.saveProfileData("") }

      ivRotateLeft.setOnClickListener { viewModel.rotateProfileImage(Direction.LEFT) }
      ivRotateRight.setOnClickListener { viewModel.rotateProfileImage(Direction.RIGHT) }

      appTb.setAssistantClickListener { assistant.playAssistantAudio(AssistantAudio.PROFILE_PICTURE_PROMPT) }

      retakeButton.setOnClickListener { selectPicture.launch(null) }
    }
  }

  private fun observeUi() {
    viewModel.profileUiState.observe(viewLifecycleOwner.lifecycle, lifecycleScope) { state ->
      when (state) {
        is ProfileUiState.Success -> showSuccessUi(state.data)
        is ProfileUiState.Error -> showErrorUi(state.throwable.message!!)
        ProfileUiState.Initial -> showInitialUi()
        ProfileUiState.Loading -> showLoadingUi()
      }
    }
  }

  private fun observeEffects() {
    viewModel.profileEffects.observe(viewLifecycleOwner.lifecycle, lifecycleScope) { effect ->
      when (effect) {
        is ProfileEffects.Navigate -> handleNavigation(effect.destination)
      }
    }
  }

  private fun showInitialUi() {
    hideLoading()
    hideRotationButtons()
    disableRetakeButton()
  }

  private fun showLoadingUi() {
    showLoading()
    hideRotationButtons()
    disableRetakeButton()
  }

  private fun showSuccessUi(bitmapPath: String?) {
    hideLoading()
    showRotationButtons()
    enableRetakeButton()

    if (!bitmapPath.isNullOrEmpty()) {
      val bitmap = BitmapFactory.decodeFile(bitmapPath)
      binding.mainProfilePictureIv.apply {
        scaleType = ImageView.ScaleType.FIT_XY
        setImageBitmap(bitmap)
        setOnClickListener(null)
      }
    }
  }

  private fun showErrorUi(message: String) {
    hideLoading()
    hideRotationButtons()
    disableRetakeButton()
  }

  private fun handleNavigation(destination: Destination) {
    when (destination) {
      Destination.MandatoryDataFlow -> navigateToSelectGenderFragment()
      Destination.Dashboard -> navigateToDashboard()
    }
  }

  private fun navigateToSelectGenderFragment() {
    findNavController().navigate(R.id.action_profileFragment_to_mandatoryDataFlow)
  }

  private fun navigateToDashboard() {
    findNavController().navigate(R.id.action_global_dashboardActivity4)
  }

  private fun showLoading() {
    binding.loadingPb.visible()
    binding.profilePictureNextBtn.gone()
    binding.retakeButton.disable()
  }

  private fun hideRotationButtons() {
    binding.ivRotateLeft.gone()
    binding.ivRotateRight.gone()
  }

  private fun showRotationButtons() {
    binding.ivRotateLeft.visible()
    binding.ivRotateRight.visible()
  }

  private fun enableRetakeButton() {
    binding.retakeButton.enable()
  }

  private fun disableRetakeButton() {
    binding.retakeButton.disable()
  }

  private fun hideLoading() {
    binding.loadingPb.gone()
    binding.profilePictureNextBtn.visible()
  }

    override fun assistantAvailable() {
        lifecycleScope.launch {
            requireContext().dataStore.doOnlyOnce(audioTag) {
                val interaction = StateInteractionRequest.Builder().setState("IDLE").build()
                Zabaan.getInstance().playInteraction(interaction)
            }
        }
    }

    override fun assistantClicked() {}

    override fun assistantLanguageNotSupported() {}
}
