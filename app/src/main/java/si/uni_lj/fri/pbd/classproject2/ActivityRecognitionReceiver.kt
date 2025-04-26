package si.uni_lj.fri.pbd.classproject2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

// When activity changes, it sends an Intent to SensingService with the help of a Worker
class ActivityRecognitionReceiver : BroadcastReceiver() {

    // What happens when Receiver gets an activity change from Google API
    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            result?.transitionEvents?.forEach { event ->
                val activityType = event.activityType
                val transitionType = event.transitionType

                // We set the default to STILL
                val activityName = when (activityType) {
                    DetectedActivity.RUNNING -> "RUNNING"
                    DetectedActivity.ON_BICYCLE -> "CYCLING"
                    DetectedActivity.WALKING -> "WALKING"
                    DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
                    DetectedActivity.STILL, DetectedActivity.UNKNOWN -> "STILL"
                    else -> "STILL"
                }

                Log.d("ActivityReceiver", "Activity: $activityName, Transition: $transitionType")

                if (transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                    // Schedule the work for the new activity
                    scheduleActivityTransition(context, activityName)
                } else if (transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                }
            }
        }
    }

    // Helper function to schedule the activity transition Worker
    private fun scheduleActivityTransition(context: Context, activityName: String) {
        val workRequest: WorkRequest = OneTimeWorkRequest.Builder(ActivityRecognitionWorker::class.java)
            .setInputData(
                androidx.work.Data.Builder().putString("activity_name", activityName).build()
            )
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
