package com.microsoft.research.karya.ui.splashScreen

import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability
import com.microsoft.research.karya.R
import com.microsoft.research.karya.databinding.FragmentSplashScreenBinding
import com.microsoft.research.karya.ui.Destination
import com.microsoft.research.karya.ui.MainActivity
import com.microsoft.research.karya.utils.extensions.observe
import com.microsoft.research.karya.utils.extensions.viewBinding
import com.microsoft.research.karya.utils.extensions.viewLifecycle
import com.microsoft.research.karya.utils.extensions.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashScreenFragment : Fragment(R.layout.fragment_splash_screen) {

  private val binding by viewBinding(FragmentSplashScreenBinding::bind)
  private val viewModel by viewModels<SplashViewModel>()
  private lateinit var navController: NavController

  private val appUpdateManager: AppUpdateManager by lazy {
    AppUpdateManagerFactory.create(requireContext())
  }

  private val UPDATE_REQUEST_CODE = 1

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    if (GoogleApiAvailabilityLight.getInstance()
        .isGooglePlayServicesAvailable(requireContext()) == ConnectionResult.SUCCESS)
      checkUpdates()
    else setupSplashScreen()
  }

  private fun checkUpdates() {

    // Returns an intent object that you use to check for an update.
    val appUpdateInfoTask = appUpdateManager.appUpdateInfo

    // Checks that the platform will allow the specified type of update.
    appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
      if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
      ) {
        appUpdateManager.startUpdateFlowForResult(
          // Pass the intent that is returned by 'getAppUpdateInfo()'.
          appUpdateInfo,
          // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
          AppUpdateType.IMMEDIATE,
          // The current activity making the update request.
          requireActivity(),
          // Include a request code to later monitor this update request.
          UPDATE_REQUEST_CODE
        )
      } else {
        // If no updates available proceed
        setupSplashScreen()
      }
    }.addOnFailureListener { setupSplashScreen() }

  }


  private fun setupSplashScreen() {
    navController = findNavController()
    handleNavigation()
    observeEffects()

    viewModel.navigate()
  }

  private fun handleNavigation() {
    viewModel.splashDestination.observe(viewLifecycle, viewLifecycleScope) { destination ->
      when (destination) {
        Destination.AccessCodeFlow -> navigateToAccessCodeFlow()
        Destination.UserSelection -> navigateToUserSelection()
        Destination.LoginFlow -> navigateToLoginFlow()
        Destination.Dashboard -> navigateToDashboard()
        Destination.TempDataFlow -> navigateToTempDataFlow()
        Destination.MandatoryDataFlow -> navigateToMandatoryDataFlow()
        Destination.Splash -> {}
      }
    }
  }

  private fun observeEffects() {
    viewModel.splashEffects.observe(viewLifecycle, viewLifecycleScope) { effect ->
      when (effect) {
        is SplashEffects.UpdateLanguage -> updateActivityLanguage(effect.language)
      }
    }
  }

  private fun updateActivityLanguage(language: String) {
    (requireActivity() as MainActivity).setActivityLocale(language)
  }

  private fun navigateToUserSelection() {
    // navController.navigate(R.id.action_splashScreenFragment_to_userSelectionFlow)
  }

  private fun navigateToAccessCodeFlow() {
    navController.navigate(R.id.action_splashScreenFragment2_to_accessCodeFragment2)
  }

  private fun navigateToDashboard() {
    navController.navigate(R.id.action_global_dashboardActivity4)
  }

  private fun navigateToLoginFlow() {
    navController.navigate(R.id.action_splashScreenFragment2_to_loginFlow2)
  }

  private fun navigateToTempDataFlow() {
    navController.navigate(R.id.action_splashScreenFragment2_to_tempDataFlow)
  }

  private fun navigateToMandatoryDataFlow() {
    navController.navigate(R.id.action_splashScreenFragment2_to_mandatoryDataFlow)
  }

  // Checks that the update is not stalled during 'onResume()'.
  // However, you should execute this check at all entry points into the app.
  override fun onResume() {
    super.onResume()

    appUpdateManager
      .appUpdateInfo
      .addOnSuccessListener { appUpdateInfo ->
        if (appUpdateInfo.updateAvailability()
          == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
        ) {
          // If an in-app update is already running, resume the update.
          appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            IMMEDIATE,
            requireActivity(),
            UPDATE_REQUEST_CODE
          )
        }
      }
  }
}
