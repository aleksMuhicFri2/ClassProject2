package si.uni_lj.fri.pbd.classproject2

import android.app.AlarmManager
import android.content.Context
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.preference.DialogPreference
import si.uni_lj.fri.pbd.classproject2.databinding.DialogTimePreferenceBinding
import java.util.*
import android.util.Log

// Helper class for setting the notification time preference in settings -> visually more pleasing
class TimePreference(context: Context, attrs: AttributeSet?) : DialogPreference(context, attrs) {
    // default time set at 20:00
    private var lastHour = 20
    private var lastMinute = 0

    init {
        summaryProvider = SummaryProvider<TimePreference> { preference ->
            String.format(Locale.getDefault(), "%02d:%02d", preference.lastHour, preference.lastMinute)
        }
    }

    override fun onClick() {
        val inflater = LayoutInflater.from(context)
        val binding = DialogTimePreferenceBinding.inflate(inflater)

        val titleView = binding.dialogTitle
        val timePicker = binding.timePicker
        titleView.text = title

        // Set the time picker to the last selected time
        timePicker.setIs24HourView(true)
        timePicker.hour = lastHour
        timePicker.minute = lastMinute

        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        val positiveButton = binding.btnOk
        val negativeButton = binding.btnCancel

        // Set listeners for the buttons
        positiveButton.setOnClickListener {
            lastHour = timePicker.hour
            lastMinute = timePicker.minute
            val newTime = "$lastHour:$lastMinute"
            persistString(newTime) // Save the new time to SharedPreferences
            notifyChanged() // Update the preference in settings UI

            // After the time is updated, re-schedule the reminder with the new time
            scheduleReminder(context, newTime)
            Log.d("TimePreference", "Reminder scheduled for $newTime")

            dialog.dismiss()
        }

        negativeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Show as summary for the preferences
    override fun onSetInitialValue(defaultValue: Any?) {
        val persisted = getPersistedString(null)

        val time = persisted ?: defaultValue as? String ?: "20:00"
        val parts = time.split(":")
        lastHour = parts.getOrNull(0)?.toIntOrNull() ?: 20
        lastMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        if (persisted == null) {
            persistString(time)
        }

        // Schedule the reminder for the initially set time
        scheduleReminder(context, time)
    }

    // This method schedules the reminder based on the user-selected time.
    private fun scheduleReminder(context: Context, reminderTime: String) {
        // Cancel any existing alarm with the same PendingIntent
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        val timeParts = reminderTime.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        // Set up the calendar for the new alarm time
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
}
