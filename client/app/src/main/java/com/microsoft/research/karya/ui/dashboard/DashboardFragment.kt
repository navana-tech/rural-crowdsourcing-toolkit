package com.microsoft.research.karya.ui.dashboard

import android.animation.Animator
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.microsoft.research.karya.R
import com.microsoft.research.karya.data.model.karya.enums.ScenarioType
import com.microsoft.research.karya.data.model.karya.modelsExtra.TaskInfo
import com.microsoft.research.karya.databinding.FragmentDashboardBinding
import com.microsoft.research.karya.ui.base.SessionFragment
import com.microsoft.research.karya.utils.extensions.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val UNIQUE_SYNC_WORK_NAME = "syncWork"

enum class ERROR_TYPE {
  SYNC_ERROR, TASK_ERROR
}

enum class ERROR_LVL {
  WARNING, ERROR
}

@AndroidEntryPoint
class DashboardFragment : SessionFragment(R.layout.fragment_dashboard) {

  override val TAG: String = "DASHBOARD_FRAGMENT"
  val binding by viewBinding(FragmentDashboardBinding::bind)
  val viewModel: DashboardViewModel by viewModels()
  private lateinit var syncWorkRequest: OneTimeWorkRequest
  private var userRefresh = false

  private var dialog: AlertDialog? = null

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
    setupWorkRequests()
    observeUi()
    viewModel.getAllTasks()
  }

  private fun observeUi() {
    viewModel.dashboardUiState.observe(viewLifecycle, viewLifecycleScope) { dashboardUiState ->
      Log.d("dashboardState", dashboardUiState.toString())
      when (dashboardUiState) {
        is DashboardUiState.Success -> {
            if (dashboardUiState.userTriggered) {
                showSuccessUi(dashboardUiState.data)
            } else {
                updateTaskList(dashboardUiState.data)
            }
        }
        is DashboardUiState.Error -> showErrorUi(
          dashboardUiState.throwable,
          ERROR_TYPE.TASK_ERROR,
          ERROR_LVL.ERROR
        )
        DashboardUiState.Loading -> showLoadingUi()
      }
    }

    WorkManager.getInstance(requireContext())
        .getWorkInfosForUniqueWorkLiveData(UNIQUE_SYNC_WORK_NAME)
        .observe(viewLifecycleOwner) { workInfoList ->
            if (workInfoList.isEmpty()) return@observe
            val workInfo = workInfoList[0] ?: return@observe

            if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                viewLifecycleScope.launch { viewModel.refreshList() }
            }

            if (workInfo.state == WorkInfo.State.RUNNING) {
                viewModel.setLoading()
            }
        }
  }

  override fun onSessionExpired() {
    WorkManager.getInstance(requireContext()).cancelAllWork()
    super.onSessionExpired()
  }

  private fun setupWorkRequests() {
    // TODO: SHIFT IT FROM HERE
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    syncWorkRequest = OneTimeWorkRequestBuilder<DashboardSyncWorker>()
      .setConstraints(constraints)
      .build()
  }

  private fun setupViews() {
    with(binding) {
      tasksRv.apply {
        adapter = TaskListAdapter(emptyList(), ::onDashboardItemClick)
        layoutManager = LinearLayoutManager(context)
      }

      refreshLl.setOnClickListener { syncWithServer() }

      appTb.setProfileClickListener { findNavController().navigate(R.id.action_global_tempDataFlow) }
      loadProfilePic()
    }
  }

  private fun syncWithServer() {
    setupWorkRequests()
    WorkManager.getInstance(requireContext()).enqueueUniqueWork(UNIQUE_SYNC_WORK_NAME, ExistingWorkPolicy.KEEP, syncWorkRequest)
    viewModel.triggerRefreshOnNextUpdate()
  }

  private fun showSuccessUi(data: DashboardStateSuccess) {
      hideLoading()

      with(binding) {
          refreshLl.enable()
          refreshLl.isClickable = true
          lottieRefresh.setAnimation(R.raw.refresh_success)
          lottieRefresh.addAnimatorListener(lottieRefreshUpdateListener)
          tvRefresh.setText(R.string.tasks_updated)
          tvRefresh.setTextColor(ContextCompat.getColor(requireContext(), R.color.refreshSuccessColor))
          lottieRefresh.playAnimation()
      }
      updateTaskList(data)
  }

  private fun updateTaskList(data: DashboardStateSuccess) {
      data.apply {
          (binding.tasksRv.adapter as TaskListAdapter).updateList(taskInfoData)
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

  private fun showSyncDialogueIfRequired(data: DashboardStateSuccess) {
      // Show a dialog box to sync with server if completed tasks and internet available
      if (requireContext().isNetworkAvailable()) {
          for (taskInfo in data.taskInfoData) {
              if (taskInfo.taskStatus.completedMicrotasks > 0) {
                  showDialogueToSync()
                  return
              }
          }
      }
  }

  private fun showDialogueToSync() {
    if (dialog != null && dialog!!.isShowing) return

    val builder: AlertDialog.Builder? = activity?.let {
      AlertDialog.Builder(it)
    }

    builder?.setMessage(R.string.s_sync_prompt_message)

    // Set buttons
    builder?.apply {
      setPositiveButton(R.string.s_yes
      ) { _, _ ->
        syncWithServer()
        dialog!!.dismiss()
      }
      setNegativeButton(R.string.s_no, null)
    }

    dialog = builder?.create()
    dialog!!.show()
  }

  private fun showErrorUi(throwable: Throwable, errorType: ERROR_TYPE, errorLvl: ERROR_LVL) {
    hideLoading()
    showError(throwable.message ?: "Some error Occurred", errorType, errorLvl)
  }

  private fun showError(message: String, errorType: ERROR_TYPE, errorLvl: ERROR_LVL) {
    if (errorType == ERROR_TYPE.SYNC_ERROR) {
      WorkManager.getInstance(requireContext()).cancelAllWork()
//      with(binding) {
//        syncErrorMessageTv.text = message
//
//        when (errorLvl) {
//          ERROR_LVL.ERROR -> syncErrorMessageTv.setTextColor(Color.RED)
//          ERROR_LVL.WARNING -> syncErrorMessageTv.setTextColor(Color.YELLOW)
//        }
//        syncErrorMessageTv.visible()
//      }
    }
  }

  private fun showLoadingUi() {
    showLoading()
    // binding.syncErrorMessageTv.gone()
  }

  private fun showLoading() {
      with(binding) {
          refreshLl.disable()
          refreshLl.isClickable = false
          lottieRefresh.setAnimation(R.raw.refresh_loading)
          tvRefresh.setText(R.string.refreshing)
          lottieRefresh.playAnimation()
      }
  }

  private fun hideLoading() {
      with(binding) {
          refreshLl.enable()
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
        val profilePicPath =
          authManager.getLoggedInWorker().profilePicturePath ?: return@withContext
        val bitmap = BitmapFactory.decodeFile(profilePicPath)

        withContext(Dispatchers.Main.immediate) { binding.appTb.setProfilePicture(bitmap) }
      }
    }
  }

  fun onDashboardItemClick(task: TaskInfo) {
    if (!task.isGradeCard && task.taskStatus.assignedMicrotasks > 0) {
      val taskId = task.taskID
      val action = with(DashboardFragmentDirections) {
        when (task.scenarioName) {
          ScenarioType.SPEECH_DATA -> actionDashboardActivityToNavanaSpeechDataMainFragment(taskId)
          ScenarioType.XLITERATION_DATA -> actionDashboardActivityToUniversalTransliterationMainFragment(taskId)
          ScenarioType.SPEECH_VERIFICATION -> actionDashboardActivityToSpeechVerificationFragment(taskId)
          ScenarioType.IMAGE_TRANSCRIPTION -> actionDashboardActivityToImageTranscription(taskId)
          ScenarioType.IMAGE_LABELLING -> actionDashboardActivityToImageLabelling(taskId)
          else -> null
        }
      }
      if (action != null) findNavController().navigate(action)
    }
  }
}
