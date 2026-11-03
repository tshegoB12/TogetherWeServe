package com.togetherweserve.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.togetherweserve.app.databinding.ActivityMainBinding
import com.togetherweserve.app.ui.organiser.CreateEventActivity
import com.togetherweserve.app.utils.SessionManager

/**
 * Hosts the bottom-navigation shell (Home, My Schedule, Notifications,
 * Profile, Settings) shown in the navigation diagram. An extra
 * "Create Event" FAB is revealed for users with the organiser role.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Android 13+ requires the user to grant POST_NOTIFICATIONS at runtime.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.i(TAG, "POST_NOTIFICATIONS granted=$granted")
            if (!granted) {
                Snackbar.make(binding.root, R.string.notification_permission_denied, Snackbar.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        binding.fabCreateEvent.setOnClickListener {
            startActivity(Intent(this, CreateEventActivity::class.java))
        }
        refreshOrganiserButton()

        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        refreshOrganiserButton()
    }

    /** Shows the "Create Event" button only while Organiser mode is on (Settings). */
    fun refreshOrganiserButton() {
        val isOrganiser = SessionManager(this).role == "organiser"
        binding.fabCreateEvent.visibility = if (isOrganiser) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            Log.d(TAG, "Requesting POST_NOTIFICATIONS permission")
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        private const val TAG = "TWS-Main"
    }
}