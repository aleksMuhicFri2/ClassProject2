package si.uni_lj.fri.pbd.classproject2

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import kotlin.math.sqrt
import androidx.core.content.edit

// Service that handles activity duration and step counting and sends a notification
class SensingService : Service(), SensorEventListener {

    companion object {
        private const val CHANNEL_ID = "sensing_service_channel"
        private const val NOTIFICATION_ID = 1
    }

    // Current activity -> default is "STILL"
    private var currentActivity: String = "STILL"

    // Step counter variables
    private var stepCount: Int = 0
    private var initialStepCount: Int? = null // Variable to store the initial step count

    // Mostly sensors
    private lateinit var activityRecognitionClient: ActivityRecognitionClient
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null

    // Handler for per-second notification updates
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)

            // Increment activity-specific durations based on the current activity
            when (currentActivity) {
                "RUNNING" -> {
                    val runningTime = prefs.getInt(Constants.DURATION_RUNNING, 0)
                    prefs.edit { putInt(Constants.DURATION_RUNNING, runningTime + 1) }
                }
                "CYCLING" -> {
                    val cyclingTime = prefs.getInt(Constants.DURATION_CYCLING, 0)
                    prefs.edit { putInt(Constants.DURATION_CYCLING, cyclingTime + 1) }
                }
                "WALKING" -> {
                    val walkingTime = prefs.getInt(Constants.DURATION_WALKING, 0)
                    prefs.edit { putInt(Constants.DURATION_WALKING, walkingTime + 1) }
                }
                // for these 2 both we update DURATION_SEDENTARY
                "STILL" -> {
                    val sedentaryTime = prefs.getInt(Constants.DURATION_SEDENTARY, 0)
                    prefs.edit { putInt(Constants.DURATION_SEDENTARY, sedentaryTime + 1) }
                }
                "IN_VEHICLE" -> {
                    val sedentaryTime = prefs.getInt(Constants.DURATION_SEDENTARY, 0)
                    prefs.edit { putInt(Constants.DURATION_SEDENTARY, sedentaryTime + 1) }
                }
            }
            // Rebuild and post notification every second
            updateNotification()

            handler.postDelayed(this, 1000)
        }
    }

    // Handler for step count update in the notification
    private val stepCountUpdateRunnable = object : Runnable {
        override fun run() {
            updateStepCountInNotification()
            handler.postDelayed(this, 60 * 60 * 1000) // 1 hour interval
        }
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        // Load initial step count
        PreferenceManager.getDefaultSharedPreferences(this).also {
            stepCount = it.getInt(Constants.STEPS_TAKEN, 0)
        }

        // Start foreground with initial notification
        startForeground(NOTIFICATION_ID, buildNotification())

        // Activity transition client
        activityRecognitionClient = ActivityRecognition.getClient(this)
        subscribeToActivityTransitions()

        // Gets sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        // Register for accelerometer data
        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Register for step counter sensor
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Kick off per-second loop
        handler.post(updateRunnable)

        // Start step count update every hour
        handler.post(stepCountUpdateRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                "WALKING", "RUNNING", "CYCLING", "IN_VEHICLE", "STILL" -> {
                    updateCurrentActivity(action)
                }
            }
        }
        return START_STICKY
    }

    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        handler.removeCallbacks(stepCountUpdateRunnable)
        activityRecognitionClient.removeActivityTransitionUpdates(getActivityRecognitionPendingIntent())
        accelerometerSensor?.let { sensorManager.unregisterListener(this, it) }
        stepSensor?.let { sensorManager.unregisterListener(this, it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Subscription to activity transitions from ActivityRecognition
    private fun subscribeToActivityTransitions() {
        val transitions = listOf(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build()
        )
        val req = ActivityTransitionRequest(transitions)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            activityRecognitionClient.requestActivityTransitionUpdates(
                req,
                getActivityRecognitionPendingIntent()
            )
        }
    }

    // Pending intent for ActivityRecognition
    private fun getActivityRecognitionPendingIntent(): PendingIntent {
        val intent = Intent(this, ActivityRecognitionReceiver::class.java)
        return PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Update current activity on transition ENTER
    private fun updateCurrentActivity(newActivity: String) {
        currentActivity = newActivity
        updateNotification()
    }

    // Notification construction
    private fun updateNotification() {
        val notification = buildNotification()
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(NOTIFICATION_ID, notification)
    }

    // Helper function to build notification
    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("fragment_to_show", "DashboardFragment")
        }
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Activity Tracking")
            .setContentText("Activity: $currentActivity  \nSteps: $stepCount")
            .setSmallIcon(R.drawable.app_icon)
            .setContentIntent(pi)
            .setOnlyAlertOnce(true)
            .build()
    }

    // Creating a channel for the notification
    private fun createNotificationChannel() {
        val chan = NotificationChannel(
            CHANNEL_ID,
            "Sensing Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(chan)
    }

    // We use this to update the step count and measure acceleration
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val total = event.values[0].toInt()
                // Initialize initial step count when it's first detected
                if (initialStepCount == null) initialStepCount = total
                val counted = total - (initialStepCount ?: total)
                if (counted != stepCount) {
                    stepCount = counted
                    PreferenceManager.getDefaultSharedPreferences(this)
                        .edit {
                            putInt(Constants.STEPS_TAKEN, stepCount)
                        }
                }
            }

            // Linear seems to work much better than the other one
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                // Calculate the magnitude
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val acceleration = sqrt(x * x + y * y + z * z)

                // Store total linear acceleration and number of readings for the current activity
                val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val totalAccelerationKey = when (currentActivity) {
                    "RUNNING" -> Constants.TOTAL_ACCELERATION_RUNNING
                    "CYCLING" -> Constants.TOTAL_ACCELERATION_CYCLING
                    else -> return // Skip if the current activity is neither RUNNING nor CYCLING
                }
                val numAccelerationKey = when (currentActivity) {
                    "RUNNING" -> Constants.NUM_ACCELERATION_RUNNING
                    "CYCLING" -> Constants.NUM_ACCELERATION_CYCLING
                    else -> return // Same here as above
                }

                // Load existing values for total acceleration and count
                val totalAcceleration = prefs.getFloat(totalAccelerationKey, 0f) + acceleration
                val numAcceleration = prefs.getInt(numAccelerationKey, 0) + 1

                // Save the updated values back to SharedPreferences
                prefs.edit {
                    putFloat(totalAccelerationKey, totalAcceleration)
                    putInt(numAccelerationKey, numAcceleration)
                }
            }
        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* nothing to see here */ }

    // Helper to update step count in notification
    private fun updateStepCountInNotification() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        stepCount = prefs.getInt(Constants.STEPS_TAKEN, 0)
        updateNotification()
    }
}
