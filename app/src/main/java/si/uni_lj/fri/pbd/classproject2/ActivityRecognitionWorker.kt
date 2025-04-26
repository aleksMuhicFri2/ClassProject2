package si.uni_lj.fri.pbd.classproject2

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log

// Helper worker for ActivityRecognitionReceiver which sends the activity change to SensingService
class ActivityRecognitionWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // Get the activity name passed as input
        val activityName = inputData.getString("activity_name") ?: return Result.failure()

        Log.d("ActivityRecognitionWorker", "Starting activity: $activityName")

        // Tell SensingService the new activity
        val serviceIntent = Intent(applicationContext, SensingService::class.java).apply {
            action = activityName
        }

        applicationContext.startService(serviceIntent)

        return Result.success()
    }
}
