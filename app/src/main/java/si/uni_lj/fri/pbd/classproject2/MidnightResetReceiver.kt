package si.uni_lj.fri.pbd.classproject2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.core.content.edit

// Midnight BroadcastReceiver to update historic data and reset the present day data
class MidnightResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit {

            // Shift step and activity data from days 1–6 to a day forward
            for (i in 6 downTo 1) {
                putInt("${i + 1}_days_behind_steps", prefs.getInt("${i}_days_behind_steps", 0))
                putInt("${i + 1}_days_behind_idle", prefs.getInt("${i}_days_behind_idle", 0))
                putInt("${i + 1}_days_behind_walking", prefs.getInt("${i}_days_behind_walking", 0))
                putInt("${i + 1}_days_behind_running", prefs.getInt("${i}_days_behind_running", 0))
                putInt("${i + 1}_days_behind_cycling", prefs.getInt("${i}_days_behind_cycling", 0))
            }

            // Store today's values as "one_day_behind" values
            putInt(Constants.ONE_DAY_BEHIND_STEPS, prefs.getInt(Constants.STEPS_TAKEN, 0))
            putInt(Constants.ONE_DAY_BEHIND_IDLE, prefs.getInt(Constants.DURATION_SEDENTARY, 0))
            putInt(Constants.ONE_DAY_BEHIND_WALKING, prefs.getInt(Constants.DURATION_WALKING, 0))
            putInt(Constants.ONE_DAY_BEHIND_RUNNING, prefs.getInt(Constants.DURATION_RUNNING, 0))
            putInt(Constants.ONE_DAY_BEHIND_CYCLING, prefs.getInt(Constants.DURATION_CYCLING, 0))

            // Reset current day values
            putInt(Constants.STEPS_TAKEN, 0)
            putInt(Constants.DURATION_RUNNING, 0)
            putInt(Constants.DURATION_CYCLING, 0)
            putInt(Constants.DURATION_WALKING, 0)
            putInt(Constants.DURATION_SEDENTARY, 0)
            putFloat(Constants.TOTAL_ACCELERATION_RUNNING, 0f)
            putInt(Constants.NUM_ACCELERATION_RUNNING, 0)
            putFloat(Constants.TOTAL_ACCELERATION_CYCLING, 0f)
            putInt(Constants.NUM_ACCELERATION_CYCLING, 0)

        }
        Log.d("Midnight Update", "Updating and resetting data")
    }
}