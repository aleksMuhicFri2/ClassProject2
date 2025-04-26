package si.uni_lj.fri.pbd.classproject2

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import si.uni_lj.fri.pbd.classproject2.ui.custom.PieChartView.ActivityData
import si.uni_lj.fri.pbd.classproject2.databinding.FragmentDashboardBinding

// Dashboard Fragment -> handles today's UI and is the "main" fragment
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private lateinit var binding: FragmentDashboardBinding
    private val handler = Handler(Looper.getMainLooper())

    // variables for easier handling of data from Shared Preferences
    private var stepsTaken = 0
    private var walkingTime = 0
    private var runningTime = 0
    private var cyclingTime = 0
    private var sedentaryTime = 0
    private var totalRunningAcceleration = 0f
    private var numRunningAcceleration = 0
    private var totalCyclingAcceleration = 0f
    private var numCyclingAcceleration = 0

    private lateinit var sharedPreferences: SharedPreferences

    // Runs the updates every second
    private val updateRunnable = object : Runnable {
        override fun run() {
            // Only update activity times if tracking is enabled
            if (isTrackingEnabled()) {
                loadData() // Load data from SharedPreferences
                updateUI() // Update UI elements
            }
            // Continue updating every second
            handler.postDelayed(this, 1000)
        }
    }

    // Initialize the fragment
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentDashboardBinding.bind(view)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        loadData() // Load data from SharedPreferences initially
        updateUI() // Initialize UI with loaded data
        handler.post(updateRunnable) // Start updating data every second
    }

    // Stops updates when the fragment is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
    }

    private fun isTrackingEnabled(): Boolean {
        // Check if tracking is enabled in settings
        return sharedPreferences.getBoolean("pref_start_tracking", false)
    }

    // Loads needed data from SharedPreferences -> look up Constants file for clarity
    private fun loadData() {
        stepsTaken = sharedPreferences.getInt(Constants.STEPS_TAKEN, 0)
        walkingTime = sharedPreferences.getInt(Constants.DURATION_WALKING, 0)
        runningTime = sharedPreferences.getInt(Constants.DURATION_RUNNING, 0)
        cyclingTime = sharedPreferences.getInt(Constants.DURATION_CYCLING, 0)
        sedentaryTime = sharedPreferences.getInt(Constants.DURATION_SEDENTARY, 0)
        totalRunningAcceleration = sharedPreferences.getFloat(Constants.TOTAL_ACCELERATION_RUNNING, 0f)
        numRunningAcceleration = sharedPreferences.getInt(Constants.NUM_ACCELERATION_RUNNING, 0)
        totalCyclingAcceleration = sharedPreferences.getFloat(Constants.TOTAL_ACCELERATION_CYCLING, 0f)
        numCyclingAcceleration = sharedPreferences.getInt(Constants.NUM_ACCELERATION_CYCLING, 0)
    }

    // Updates the bottom values on the screen
    private fun updateUI() {
        val avgRunningAcc = if (numRunningAcceleration > 0) totalRunningAcceleration / numRunningAcceleration else 0f
        val avgCyclingAcc = if (numCyclingAcceleration > 0) totalCyclingAcceleration / numCyclingAcceleration else 0f

        binding.tvAvgAccelerationRunning.text = getString(R.string.avg_acceleration_running, avgRunningAcc)
        binding.tvAvgAccelerationCycling.text = getString(R.string.avg_acceleration_cycling, avgCyclingAcc)
        binding.tvStepsTaken.text = getString(R.string.steps_taken, stepsTaken)

        // Update the pie chart data with the current data
        updatePieChart()
    }

    // Helper function to update the pie chart with the current data
    private fun updatePieChart() {
        val total = walkingTime + runningTime + cyclingTime + sedentaryTime

        val percentages = if (total > 0) {
            mapOf(
                "Walking" to ActivityData(walkingTime, walkingTime.toFloat() / total * 100f),
                "Running" to ActivityData(runningTime, runningTime.toFloat() / total * 100f),
                "Cycling" to ActivityData(cyclingTime, cyclingTime.toFloat() / total * 100f),
                "Sedentary" to ActivityData(sedentaryTime, sedentaryTime.toFloat() / total * 100f)
            )
        } else {
            mapOf(
                "Walking" to ActivityData(0, 0f),
                "Running" to ActivityData(0, 0f),
                "Cycling" to ActivityData(0, 0f),
                "Sedentary" to ActivityData(0, 0f)
            )
        }

        binding.pieChartView.activityData = percentages
    }
}
