package si.uni_lj.fri.pbd.classproject2

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import android.util.Log

// Sends a notification if the user hasn't reached their step goal
// Its usually a little late, but always send it within the same minute
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        Log.d("ReminderAlarmReceiver", "Alarm received, checking step count...")
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        // step count from preferences
        val stepCount = sharedPreferences.getInt(Constants.STEPS_TAKEN, 0)

        // step goal from settings
        val stepGoalString = sharedPreferences.getString("pref_step_goal", "2000") ?: "2000"
        val stepGoal = stepGoalString.toInt()  // Convert the string from preferences to an Int

        // If the goal is not reached, send the notification
        if (stepCount < stepGoal) {
            sendNotification(context)
        }
    }

    // function to build and send a notification
    private fun sendNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // opens DashboardFragment if we press it
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("fragment_to_show", "DashboardFragment")
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "reminder_channel")
            .setContentTitle("Activity Reminder")
            .setContentText("You have not reached your step goal!")
            .setSmallIcon(R.drawable.app_icon) // My own icon :) from drawables
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, notification) // sends on ID == 2
    }
}
