package si.uni_lj.fri.pbd.classproject2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.PreferenceManager
import android.widget.Toast
import androidx.core.content.edit
import androidx.preference.EditTextPreference
import android.util.Log

// Fragment for the settings
class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        private const val REQUEST_CODE_ACTIVITY_PERM = 101
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val ctx = requireContext()
        val svcIntent = Intent(ctx, SensingService::class.java)

        // Toggle to start/stop the SensingService
        findPreference<SwitchPreferenceCompat>("pref_start_tracking")
            ?.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean

                if (enabled) {
                    // Check runtime permission for ACTIVITY_RECOGNITION
                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACTIVITY_RECOGNITION)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        // Ask for it
                        ActivityCompat.requestPermissions(
                            requireActivity(),
                            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                            REQUEST_CODE_ACTIVITY_PERM
                        )
                        false
                    } else {
                        // Permission already granted → start service
                        ContextCompat.startForegroundService(ctx, svcIntent)
                        Log.d("SettingsFragment", "Tracking service started")
                        true
                    }
                } else {
                    // Stop the service
                    ctx.stopService(svcIntent)
                    Log.d("SettingsFragment", "Tracking service stopped")
                    true
                }
            }

        // Set step goal
        findPreference<EditTextPreference>("pref_step_goal")
            ?.setOnPreferenceChangeListener { _, newValue ->
                val goal = newValue.toString()
                PreferenceManager.getDefaultSharedPreferences(ctx).edit {
                    putString("pref_step_goal", goal)
                    apply()
                }
                true
            }

        //  Reset app data
        findPreference<Preference>("pref_reset_app")
            ?.setOnPreferenceClickListener {
                PreferenceManager
                    .getDefaultSharedPreferences(ctx)
                    .edit {
                        clear()
                        apply()
                    }
                Toast.makeText(ctx, "All data cleared", Toast.LENGTH_SHORT).show()
                true
            }
    }

    // Helper to handle the permission callback for the tracking service
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_ACTIVITY_PERM) {
            val granted = grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                ContextCompat.startForegroundService(
                    requireContext(),
                    Intent(requireContext(), SensingService::class.java)
                )
                findPreference<SwitchPreferenceCompat>("pref_start_tracking")
                    ?.isChecked = true
            } else {
                Toast.makeText(
                    requireContext(),
                    "Activity recognition permission is required to track activity",
                    Toast.LENGTH_LONG
                ).show()
                findPreference<SwitchPreferenceCompat>("pref_start_tracking")
                    ?.isChecked = false
            }
        }
    }
}
