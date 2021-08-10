package com.microsoft.research.karya.ui.dashboard

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.microsoft.research.karya.R
import com.microsoft.research.karya.data.manager.AuthManager
import com.microsoft.research.karya.data.model.karya.modelsExtra.TaskInfo
import com.microsoft.research.karya.databinding.FragmentDashboardBinding
import com.microsoft.research.karya.ui.scenarios.speechData.SpeechDataMain
import com.microsoft.research.karya.ui.scenarios.speechVerification.SpeechVerificationMain
import com.microsoft.research.karya.ui.scenarios.textToTextTranslation.TextToTextTranslationMain
import com.microsoft.research.karya.utils.PreferenceKeys
import com.microsoft.research.karya.utils.extensions.dataStore
import com.microsoft.research.karya.utils.extensions.gone
import com.microsoft.research.karya.utils.extensions.observe
import com.microsoft.research.karya.utils.extensions.viewBinding
import com.microsoft.research.karya.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

  val binding by viewBinding(FragmentDashboardBinding::bind)
  val viewModel: DashboardViewModel by viewModels()

  @Inject lateinit var authManager: AuthManager

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupViews()
    observeUi()
    fetchTasksOnFirstRun()
  }

  override fun onResume() {
    super.onResume()
    viewModel.getAllTasks()
  }

  private fun setupViews() {
    with(binding) {
      // TODO: Convert this to one string instead of joining multiple strings
      /*
      val syncText =
        "${getString(R.string.s_get_new_tasks)} - " +
          "${getString(R.string.s_submit_completed_tasks)} - " +
          "${getString(R.string.s_update_verified_tasks)} - " +
          getString(R.string.s_update_earning)

      syncPromptTv.text = syncText
      */

      tasksRv.apply {
        adapter = TaskListAdapter(emptyList(), ::onDashboardItemClick)
        layoutManager = LinearLayoutManager(context)
      }

      tvCheckUpdates.setOnClickListener { viewModel.syncWithServer() }

      appTb.setProfileClickListener { findNavController().navigate(R.id.action_global_tempDataFlow) }
      loadProfilePic()
    }
  }

  private fun observeUi() {
    viewModel.dashboardUiState.observe(lifecycle, lifecycleScope) { dashboardUiState ->
      when (dashboardUiState) {
        is DashboardUiState.Success -> showSuccessUi(dashboardUiState.data)
        is DashboardUiState.Error -> showErrorUi(dashboardUiState.throwable)
        DashboardUiState.Loading -> showLoadingUi()
      }
    }
  }

  private fun showSuccessUi(data: DashboardStateSuccess) {
    hideLoading()
    data.apply {
      (binding.tasksRv.adapter as TaskListAdapter).updateList(taskInfoData)
      // Show total credits if it is greater than 0
      if (totalCreditsEarned > 0.0f) {
        binding.rupeesEarnedCl.visible()
        binding.rupeesEarnedTv.text = "%.2f".format(totalCreditsEarned)
      } else {
        binding.rupeesEarnedCl.gone()
      }
    }
  }

  private fun showErrorUi(throwable: Throwable) {
    hideLoading()
  }

  private fun showLoadingUi() {
    showLoading()
  }

  private fun showLoading() {}

  private fun hideLoading() {}

  private fun loadProfilePic() {
    //    binding.appTb.showProfilePicture()

    lifecycleScope.launchWhenStarted {
      withContext(Dispatchers.IO) {
        val profilePicPath = authManager.fetchLoggedInWorker().profilePicturePath ?: return@withContext
        val bitmap = BitmapFactory.decodeFile(profilePicPath)

        withContext(Dispatchers.Main.immediate) { binding.appTb.setProfilePicture(bitmap) }
      }
    }
  }

  fun onDashboardItemClick(task: TaskInfo) {
    val nextIntent =
      when (task.scenarioName) {
        // TODO: MAKE THIS GENERAL ONCE API RESPONSE UPDATES
        // Use [ScenarioType] enum once we migrate to it.
        "SPEECH_DATA" -> Intent(requireContext(), SpeechDataMain::class.java)
        "SPEECH_VERIFICATION" -> Intent(requireContext(), SpeechVerificationMain::class.java)
        "TEXT_TRANSLATION" -> Intent(requireContext(), TextToTextTranslationMain::class.java)
        else -> {
          throw Exception("Unimplemented scenario")
        }
      }

    nextIntent.putExtra("taskID", task.taskID)
    startActivity(nextIntent)
  }

  private fun fetchTasksOnFirstRun() {
    val firstFetchKey = booleanPreferencesKey(PreferenceKeys.IS_FIRST_FETCH)

    lifecycleScope.launchWhenStarted {
      this@DashboardFragment.requireContext().dataStore.edit { prefs ->
        val isFirstFetch = prefs[firstFetchKey] ?: true

        if (isFirstFetch) {
          viewModel.syncWithServer()
        }

        prefs[firstFetchKey] = false
      }
    }
  }
}
