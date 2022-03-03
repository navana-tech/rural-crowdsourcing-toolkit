package com.microsoft.research.karya.data.repo

import com.microsoft.research.karya.data.local.daos.MicroTaskAssignmentDao
import com.microsoft.research.karya.data.local.daos.TaskDao
import com.microsoft.research.karya.data.model.karya.enums.MicrotaskAssignmentStatus
import com.microsoft.research.karya.data.model.karya.modelsExtra.TaskInfo
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskRepository
@Inject
constructor(
  private val taskDao: TaskDao,
  private val microTaskAssignmentDao: MicroTaskAssignmentDao,
) {
  suspend fun getTaskInfo(): List<TaskInfo> =
    withContext(Dispatchers.IO) {
      val assignments = microTaskAssignmentDao.getAll()
      val tasks = taskDao.getAll()

      val taskInfoList =
        tasks.map { task ->
          val assignmentTasks = assignments.filter { assignment -> assignment.task_id == task.id }

          val assigned = assignmentTasks.count { assignment -> assignment.status == MicrotaskAssignmentStatus.ASSIGNED }
          val completed =
            assignmentTasks.count { assignment -> assignment.status == MicrotaskAssignmentStatus.COMPLETED }
          val submitted =
            assignmentTasks.count { assignment -> assignment.status == MicrotaskAssignmentStatus.SUBMITTED }
          val verified = assignmentTasks.count { assignment -> assignment.status == MicrotaskAssignmentStatus.VERIFIED }

          return@map TaskInfo(task.id, task.name, task.scenario_name, assigned, completed, submitted, verified)
        }
      return@withContext taskInfoList
    }
}
