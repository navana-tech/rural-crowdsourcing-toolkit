package com.microsoft.research.karya.ui.dashboard

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.microsoft.research.karya.R
import com.microsoft.research.karya.data.model.karya.modelsExtra.TaskInfo
import com.microsoft.research.karya.databinding.ItemDashboardCardBinding

class TaskListAdapter(
  private var tasks: List<TaskInfo>,
  private val dashboardItemClick: (task: TaskInfo) -> Unit = {},
) : RecyclerView.Adapter<TaskListAdapter.NgTaskViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NgTaskViewHolder {
    val layoutInflater = LayoutInflater.from(parent.context)
    val binding = ItemDashboardCardBinding.inflate(layoutInflater, parent, false)

    return NgTaskViewHolder(binding, dashboardItemClick)
  }

  override fun onBindViewHolder(holder: NgTaskViewHolder, position: Int) {
    holder.bind(tasks[position])
  }

  override fun getItemCount(): Int {
    return tasks.size
  }

  fun addTasks(newTasks: List<TaskInfo>) {
    val oldTaskCount = tasks.size
    val tempList = mutableListOf<TaskInfo>()
    tempList.addAll(tasks)
    tempList.addAll(newTasks)

    tasks = tempList
    notifyItemRangeInserted(oldTaskCount, newTasks.size)
  }

  fun updateList(newList: List<TaskInfo>) {
    tasks = newList
    notifyDataSetChanged()
  }

  class NgTaskViewHolder(
    private val binding: ItemDashboardCardBinding,
    private val dashboardItemClick: (task: TaskInfo) -> Unit,
  ) : RecyclerView.ViewHolder(binding.root) {

    fun bind(taskInfo: TaskInfo) {
      setText(binding, taskInfo)
      setViews(binding, taskInfo)
    }

    private fun setText(binding: ItemDashboardCardBinding, task: TaskInfo) {
      with(binding) {
        task.apply {
          val context = binding.root.context
          val total = assignedMicrotasks + completedMicrotasks + submittedMicrotasks + verifiedMicrotasks

          val available = assignedMicrotasks
          val completed = completedMicrotasks + submittedMicrotasks + verifiedMicrotasks
          val submitted = submittedMicrotasks + verifiedMicrotasks
          val verified = verifiedMicrotasks

          if (taskName.contains("Spontaneous", ignoreCase = true)) {
            binding.header.setBackgroundColor(Color.parseColor("#FF03A9F4"))
          }
          taskTitle.text = taskName
          taskSubtitle.text = context.getString(R.string.d_sentences_available, total)
          tasksAvailable.text = context.getString(R.string.d_tasks_available, available, total)
          tasksCompleted.text = context.getString(R.string.d_tasks_completed, completed, total)
          tasksSubmitted.text = context.getString(R.string.d_tasks_submitted, submitted, total)
          tasksVerified.text = context.getString(R.string.d_tasks_verified, verified, total)
        }
      }
    }

    private fun setViews(binding: ItemDashboardCardBinding, task: TaskInfo) {
      with(binding) {
        task.apply {
          root.apply {
            val clickableAndEnabled = task.assignedMicrotasks > 0
            isClickable = clickableAndEnabled
            isEnabled = clickableAndEnabled

            setOnClickListener {
              isClickable = false
              isEnabled = false
              dashboardItemClick(task)
              isClickable = true
              isEnabled = true
            }
          }

          val drawable =
            when (task.scenarioName) {
              "SPEECH_DATA" -> ContextCompat.getDrawable(root.context, R.drawable.ic_task_speech_data)
              "SPEECH_VERIFICATION" -> ContextCompat.getDrawable(root.context, R.drawable.ic_task_speech_data)
              "TEXT_TRANSLATION" -> ContextCompat.getDrawable(root.context, R.drawable.ic_task_text_data)
              else -> ContextCompat.getDrawable(root.context, R.drawable.ic_task_speech_data)
            }
          taskImage.setImageDrawable(drawable)
        }
      }
    }
  }
}
