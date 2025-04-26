package si.uni_lj.fri.pbd.classproject2

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class FullActivityTransitionSequenceTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private var currentActivity: Int? = null


    // Function to simulate sending activity transitions
    private fun sendTransition(
        context: Context,
        activityType: Int,
        transitionType: Int,
        timestampNanos: Long = SystemClock.elapsedRealtimeNanos()
    ) {
        val intent = Intent(context, ActivityRecognitionReceiver::class.java)

        val events: ArrayList<ActivityTransitionEvent> = arrayListOf()
        events.add(ActivityTransitionEvent(activityType, transitionType, timestampNanos))

        val result = ActivityTransitionResult(events)
        SafeParcelableSerializer.serializeToIntentExtra(
            result,
            intent,
            "com.google.android.location.internal.EXTRA_ACTIVITY_TRANSITION_RESULT"
        )
        context.sendBroadcast(intent)

        // Track current activity only if it's an ENTER transition
        if (transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
            currentActivity = activityType
        } else if (transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT && currentActivity == activityType) {
            currentActivity = null
        }
    }

    // Helper function to convert seconds to milliseconds
    private fun secondsToMillis(seconds: Long): Long {
        return seconds * 1000
    }

    // Function to simulate step counting increment and update SharedPreferences
    private fun simulateSteps(context: Context, increment: Int) {
        // Get the SharedPreferences instance
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        // Get the current step count, defaulting to 0 if not set
        val currentStepCount = prefs.getInt(Constants.STEPS_TAKEN, 0)

        // Update the step count by incrementing it
        val newStepCount = currentStepCount + increment

        // Save the updated step count back to SharedPreferences
        prefs.edit().putInt(Constants.STEPS_TAKEN, newStepCount).apply()
    }

    private fun maybeSimulateAcceleration(context: Context, acceleration: Float) {
        when (currentActivity) {
            DetectedActivity.RUNNING -> simulateAverageAcceleration(context, acceleration, "RUNNING")
            DetectedActivity.ON_BICYCLE -> simulateAverageAcceleration(context, acceleration, "CYCLING")
        }
    }

    // Function to simulate accelerometer data and calculate average acceleration
    private fun simulateAverageAcceleration(context: Context, newAcceleration: Float, activity: String = "RUNNING") {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val (totalKey, countKey) = when (activity) {
            "RUNNING" -> Constants.TOTAL_ACCELERATION_RUNNING to Constants.NUM_ACCELERATION_RUNNING
            "CYCLING" -> Constants.TOTAL_ACCELERATION_CYCLING to Constants.NUM_ACCELERATION_CYCLING
            else -> return // ignore other activities
        }

        val totalAcceleration = prefs.getFloat(totalKey, 0f)
        val readingCount = prefs.getInt(countKey, 0)

        val newTotal = totalAcceleration + newAcceleration
        val newCount = readingCount + 1

        prefs.edit()
            .putFloat(totalKey, newTotal)
            .putInt(countKey, newCount)
            .apply()

        val avg = newTotal / newCount
    }

    // Function to simulate random accelerometer data
    private fun getRandomAcceleration(): Float {
        // Simulate random acceleration between -2.0 and 2.0 m/s^2
        return Random.nextFloat() * 4 - 2
    }

    @Test
    fun test1() {
        // Wait for a while to setup the app
        Thread.sleep(secondsToMillis(21))

        val context = ApplicationProvider.getApplicationContext<Context>()

        // Simulate accelerometer data and steps
        sendTransition(context, DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        simulateSteps(context, 10) // Simulate 10 steps
        maybeSimulateAcceleration(context, getRandomAcceleration())

        sendTransition(context, DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_ENTER)

        // Simulate more steps and acceleration
        simulateSteps(context, 20) // Add 20 more steps
        maybeSimulateAcceleration(context, getRandomAcceleration())

        // Wait for a while to check results
        Thread.sleep(secondsToMillis(42))

        // Send exit transition at the end of the test
        sendTransition(context, DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
    }

    @Test
    fun test2() {
        // Wait for a while to setup the app
        Thread.sleep(secondsToMillis(21))

        val context = ApplicationProvider.getApplicationContext<Context>()

        // Simulate accelerometer data and steps
        sendTransition(context, DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        simulateSteps(context, 15) // Simulate 15 steps
        maybeSimulateAcceleration(context, getRandomAcceleration())
        sendTransition(context, DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_ENTER)

        // Simulate more steps and acceleration
        simulateSteps(context, 30) // Add 30 more steps
        maybeSimulateAcceleration(context, getRandomAcceleration())

        // Wait for a while to check results
        Thread.sleep(secondsToMillis(42))

        // Send exit transition at the end of the test
        sendTransition(context, DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
    }

    @Test
    fun test3() {
        // Wait for a while to setup the app
        Thread.sleep(secondsToMillis(21))

        val context = ApplicationProvider.getApplicationContext<Context>()

        sendTransition(context, DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        simulateSteps(context, 10) // Simulate 10 steps
        maybeSimulateAcceleration(context, getRandomAcceleration())
        sendTransition(context, DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_ENTER)

        Thread.sleep(secondsToMillis(60))

        simulateSteps(context, 50) // Simulate 50 steps during this period
        maybeSimulateAcceleration(context, getRandomAcceleration())

        sendTransition(context, DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        sendTransition(context, DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_ENTER)

        // Simulate more steps and acceleration
        simulateSteps(context, 40) // Simulate 40 steps
        maybeSimulateAcceleration(context, getRandomAcceleration())

        Thread.sleep(secondsToMillis(120))

        sendTransition(context, DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        sendTransition(context, DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER)

        maybeSimulateAcceleration(context, getRandomAcceleration())

        Thread.sleep(secondsToMillis(30))

        sendTransition(context, DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        sendTransition(context, DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_ENTER)

        Thread.sleep(secondsToMillis(60))

        sendTransition(context, DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT)

        // Wait for a while to check results
        Thread.sleep(secondsToMillis(42))
    }
}
