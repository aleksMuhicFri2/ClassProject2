package si.uni_lj.fri.pbd.classproject2

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import si.uni_lj.fri.pbd.classproject2.databinding.FragmentHistoryBinding
import java.util.Locale

// History fragment -> shows past data from SharedPreferences
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Initialize the fragment and show data
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = requireContext().getSharedPreferences("your_preference_file_key", Context.MODE_PRIVATE)

        // Yesterday's data
        val yesterdaySteps = prefs.getInt(Constants.ONE_DAY_BEHIND_STEPS, 0)
        val yesterdayIdle = prefs.getInt(Constants.ONE_DAY_BEHIND_IDLE, 0)
        val yesterdayWalking = prefs.getInt(Constants.ONE_DAY_BEHIND_WALKING, 0)
        val yesterdayRunning = prefs.getInt(Constants.ONE_DAY_BEHIND_RUNNING, 0)
        val yesterdayCycling = prefs.getInt(Constants.ONE_DAY_BEHIND_CYCLING, 0)

        val yesterdayTotal = yesterdayIdle + yesterdayWalking + yesterdayRunning + yesterdayCycling

        // Binds the data to output
        binding.textStepCount.text = "$yesterdaySteps steps"
        binding.textIdleTime.text = formatDurationWithPercent(yesterdayIdle, yesterdayTotal)
        binding.textWalkingTime.text = formatDurationWithPercent(yesterdayWalking, yesterdayTotal)
        binding.textRunningTime.text = formatDurationWithPercent(yesterdayRunning, yesterdayTotal)
        binding.textCyclingTime.text = formatDurationWithPercent(yesterdayCycling, yesterdayTotal)

        // Weekly data: sum of last 7 days
        val weekSteps = (1..7).sumOf { prefs.getInt("${getDayKey(it)}_steps", 0) }
        val weekIdle = (1..7).sumOf { prefs.getInt("${getDayKey(it)}_idle", 0) }
        val weekWalking = (1..7).sumOf { prefs.getInt("${getDayKey(it)}_walking", 0) }
        val weekRunning = (1..7).sumOf { prefs.getInt("${getDayKey(it)}_running", 0) }
        val weekCycling = (1..7).sumOf { prefs.getInt("${getDayKey(it)}_cycling", 0) }

        val weekTotal = weekIdle + weekWalking + weekRunning + weekCycling

        // Binds the data to output
        binding.textStepCountWeekly.text = "$weekSteps steps"
        binding.textIdleTimeWeekly.text = formatDurationWithPercent(weekIdle, weekTotal)
        binding.textWalkingTimeWeekly.text = formatDurationWithPercent(weekWalking, weekTotal)
        binding.textRunningTimeWeekly.text = formatDurationWithPercent(weekRunning, weekTotal)
        binding.textCyclingTimeWeekly.text = formatDurationWithPercent(weekCycling, weekTotal)
    }

    private fun getDayKey(day: Int): String {
        return when (day) {
            1 -> "1_day_behind"
            2 -> "2_days_behind"
            3 -> "3_days_behind"
            4 -> "4_days_behind"
            5 -> "5_days_behind"
            6 -> "6_days_behind"
            7 -> "7_days_behind"
            else -> throw IllegalArgumentException("Day must be 1–7")
        }
    }

    // Formats the output string for the activities ->
    // Time on the left and percentage on the right
    private fun formatDurationWithPercent(seconds: Int, total: Int): String {
        val formattedTime = formatDuration(seconds)
        val percent = if (total > 0) (seconds * 100.0 / total).toInt() else 0
        return "$formattedTime  •  $percent%"
    }

    // Time format Helper
    private fun formatDuration(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
