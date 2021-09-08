package com.microsoft.research.karya.ui.dashboard

import android.animation.Animator
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
import com.microsoft.research.karya.data.model.karya.enums.ScenarioType
import com.microsoft.research.karya.data.model.karya.modelsExtra.TaskInfo
import com.microsoft.research.karya.databinding.FragmentNavanaDashboardBinding
import com.microsoft.research.karya.ui.base.SessionFragment
import com.microsoft.research.karya.utils.PreferenceKeys
import com.microsoft.research.karya.utils.extensions.dataStore
import com.microsoft.research.karya.utils.extensions.observe
import com.microsoft.research.karya.utils.extensions.viewBinding
import com.microsoft.research.karya.utils.extensions.viewLifecycle
import com.microsoft.research.karya.utils.extensions.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class NavanaDashboardFragment : SessionFragment(R.layout.fragment_navana_dashboard) {

    override val TAG: String = "NAVANA_DASHBOARD_FRAGMENT"
    val binding by viewBinding(FragmentNavanaDashboardBinding::bind)
    val viewModel: NavanaDashboardViewModel by viewModels()

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
                adapter = NavanaTaskListAdapter(emptyList(), ::onDashboardItemClick)
                layoutManager = LinearLayoutManager(context)
            }

            refreshLl.setOnClickListener { viewModel.syncWithServer() }

            appTb.setProfileClickListener { findNavController().navigate(R.id.action_global_tempDataFlow) }
            loadProfilePic()
        }
    }

    private fun observeUi() {
        viewModel.dashboardUiState.observe(viewLifecycle, viewLifecycleScope) { dashboardUiState ->
            Log.d("dashboardUiState", dashboardUiState.toString())
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
            (binding.tasksRv.adapter as NavanaTaskListAdapter).updateList(taskInfoData)
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
            // Remove listener since we only need it for the last bit of the animation
            lottieRefresh.removeAnimatorListener(lottieRefreshUpdateListener)
            lottieRefresh.pauseAnimation()
            lottieRefresh.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_refresh))
            tvRefresh.setText(R.string.refresh_underline)
            tvRefresh.setTextColor(ContextCompat.getColor(requireContext(), R.color.checkUpdatesColor))
        }
    }

    private fun loadProfilePic() {
        lifecycleScope.launchWhenStarted {
            withContext(Dispatchers.IO) {
                val profilePicPath = authManager.getLoggedInWorker().profilePicturePath ?: return@withContext
                val bitmap = BitmapFactory.decodeFile(profilePicPath)

                withContext(Dispatchers.Main.immediate) { binding.appTb.setProfilePicture(bitmap) }
            }
        }
    }

    fun onDashboardItemClick(task: TaskInfo) {
        if (!task.isGradeCard && task.taskStatus.assignedMicrotasks > 0) {
            val taskId = task.taskID
            val action = with(NavanaDashboardFragmentDirections) {
                when (task.scenarioName) {
                    ScenarioType.SPEECH_DATA -> actionNavanaDashboardToNavanaSpeechData(taskId)
                    else -> null
                }
            }
            if (action != null) findNavController().navigate(action)
        }
    }


    private fun fetchTasksOnFirstRun() {
        val firstFetchKey = booleanPreferencesKey(PreferenceKeys.IS_FIRST_FETCH)

        lifecycleScope.launchWhenStarted {
            this@NavanaDashboardFragment.requireContext().dataStore.edit { prefs ->
                val isFirstFetch = prefs[firstFetchKey] ?: true

                if (isFirstFetch) {
                    viewModel.syncWithServer()
                }

                prefs[firstFetchKey] = false
            }
        }
    }
}
