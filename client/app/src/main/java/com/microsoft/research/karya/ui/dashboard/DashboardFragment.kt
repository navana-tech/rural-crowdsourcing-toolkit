package com.microsoft.research.karya.ui.dashboard

import android.animation.Animator
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.microsoft.research.karya.R
import com.microsoft.research.karya.data.manager.AuthManager
import com.microsoft.research.karya.data.model.karya.modelsExtra.TaskInfo
import com.microsoft.research.karya.databinding.FragmentDashboardBinding
import com.microsoft.research.karya.ui.base.SessionFragment
import com.microsoft.research.karya.ui.scenarios.speechData.SpeechDataMain
import com.microsoft.research.karya.ui.scenarios.speechVerification.SpeechVerificationMain
import com.microsoft.research.karya.ui.scenarios.textToTextTranslation.TextToTextTranslationMain
import com.microsoft.research.karya.utils.PreferenceKeys
import com.microsoft.research.karya.utils.extensions.dataStore
import com.microsoft.research.karya.utils.extensions.observe
import com.microsoft.research.karya.utils.extensions.viewBinding
import com.microsoft.research.karya.utils.extensions.viewLifecycle
import com.microsoft.research.karya.utils.extensions.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class DashboardFragment : SessionFragment(R.layout.fragment_dashboard) {

  override val TAG: String = "DASHBOARD_FRAGMENT"
  val binding by viewBinding(FragmentDashboardBinding::bind)
  val viewModel: DashboardViewModel by viewModels()

  private val lottieRefreshUpdateListener = object : Animator.AnimatorListener {
      override fun onAnimationStart(animation: Animator?) {
      }

      override fun onAnimationEnd(animation: Animator?) {
          hideLoading()
      }

      override fun onAnimationCancel(animation: Animator?) {
          hideLoading()
      }

      override fun onAnimationRepeat(animation: Animator?) {
      }

  }

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
      tasksRv.apply {
        adapter = TaskListAdapter(emptyList(), ::onDashboardItemClick)
        layoutManager = LinearLayoutManager(context)
      }

      refreshLl.setOnClickListener { viewModel.syncWithServer() }

      appTb.setProfileClickListener { findNavController().navigate(R.id.action_global_tempDataFlow) }
      loadProfilePic()
    }
  }

  private fun observeUi() {
    viewModel.dashboardUiState.observe(viewLifecycle, viewLifecycleScope) { dashboardUiState ->
      when (dashboardUiState) {
        is DashboardUiState.Success -> if (dashboardUiState.userTriggered) {
            showSuccessUi(dashboardUiState.data)
        } else {
            updateTasks(dashboardUiState.data)
        }
        is DashboardUiState.Error -> showErrorUi(dashboardUiState.throwable)
        DashboardUiState.Loading -> showLoadingUi()
      }
    }
  }

    private fun updateTasks(data: DashboardStateSuccess) {
        data.apply {
            (binding.tasksRv.adapter as TaskListAdapter).updateList(taskInfoData)
            // Show total credits if it is greater than 0
            if (totalCreditsEarned > 0.0f) {
                val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_coins) ?: return@apply
                binding.appTb.setEndIcon(drawable)
                binding.appTb.setEndText(requireContext().getString(R.string.rupees_d, totalCreditsEarned.toInt()))
            } else {
                binding.appTb.hideEndIcon()
                binding.appTb.hideEndText()
            }
        }
    }

  private fun showSuccessUi(data: DashboardStateSuccess) {
    updateTasks(data)

      with(binding) {
          lottieRefresh.setAnimation(R.raw.refresh_success)
          lottieRefresh.addAnimatorListener(lottieRefreshUpdateListener)
          tvRefresh.setText(R.string.tasks_updated)
          tvRefresh.setTextColor(ContextCompat.getColor(requireContext(), R.color.refreshSuccessColor))
          lottieRefresh.playAnimation()
      }
  }

  private fun showErrorUi(throwable: Throwable) {
      hideLoading()
  }

  private fun showLoadingUi() {
    showLoading()
  }

  private fun showLoading() {
      with(binding) {
          refreshLl.isClickable = false
          lottieRefresh.setAnimation(R.raw.refresh_loading)
          tvRefresh.setText(R.string.refreshing)
          lottieRefresh.playAnimation()
      }
  }

  private fun hideLoading() {
      with(binding) {
          refreshLl.isClickable = true
          lottieRefresh.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_refresh))
          // Remove listener since we only need it for the last bit of the animation
          lottieRefresh.removeAnimatorListener(lottieRefreshUpdateListener)
          tvRefresh.setText(R.string.refresh_underline)
          tvRefresh.setTextColor(ContextCompat.getColor(requireContext(), R.color.checkUpdatesColor))
          lottieRefresh.playAnimation()
      }
  }

  private fun loadProfilePic() {
    lifecycleScope.launchWhenStarted {
      withContext(Dispatchers.IO) {
        val profilePicPath = authManager.fetchLoggedInWorker().profilePicturePath ?: return@withContext
        val bitmap = BitmapFactory.decodeFile(profilePicPath)

        withContext(Dispatchers.Main.immediate) { binding.appTb.setProfilePicture(bitmap) }
      }
    }
  }

  fun onDashboardItemClick(task: TaskInfo) {
    Log.d("onDashboardItemClick", "clicked")
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
