package si.uni_lj.fri.pbd.classproject2

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
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
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.hamcrest.Matchers.containsString

@RunWith(AndroidJUnit4::class)
class FullActivityTransitionSequenceTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)


    private fun sendTransition(
        context: Context,
        activityType: Int,
        transitionType: Int,
        timestampNanos: Long = SystemClock.elapsedRealtimeNanos()
    ){
        val intent = Intent(context, ActivityRecognitionReceiver::class.java)

        val events: ArrayList<ActivityTransitionEvent> = arrayListOf()

        // create fake events
        events.add(
            ActivityTransitionEvent(
                activityType,
                transitionType,
                timestampNanos
            )
        )

        // finally, serialize and send
        val result = ActivityTransitionResult(events)
        SafeParcelableSerializer.serializeToIntentExtra(
            result,
            intent,
            "com.google.android.location.internal.EXTRA_ACTIVITY_TRANSITION_RESULT"
        )
        context.sendBroadcast(intent)
    }


    @Test
    fun simulateActivityTransitions() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        Thread.sleep(20000)

        sendTransition(context, DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        sendTransition(context, DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_ENTER)

        Thread.sleep(120000)

    }
}