package com.togetherweserve.app

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.auth.FirebaseAuth
import com.togetherweserve.app.data.local.AppDatabase
import com.togetherweserve.app.data.repository.EventRepository
import com.togetherweserve.app.utils.NotificationTopics
import com.togetherweserve.app.utils.SessionManager
import com.togetherweserve.app.utils.SupportedLanguages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Application entry point for TogetherWeServe. */
class TogetherWeServeApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    // Lives as long as the process; used for background sync work.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val session = SessionManager(this)

        // Restore the user's selected South African language before the first screen is shown.
        val language = SupportedLanguages.normalise(session.language)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
        Log.i(TAG, "App started. Language=$language")

        // Event alerts follow the user's "Event reminders" preference from Settings.
        NotificationTopics.apply(session.eventRemindersEnabled)

        registerOfflineSync()
    }

    /**
     * Offline mode with Sync: whenever connectivity returns, anywhere in the app,
     * replay the actions queued in RoomDB and refresh the schedule from the REST API.
     */
    private fun registerOfflineSync() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.i(TAG, "Network available - flushing offline queue")
                    appScope.launch {
                        try {
                            delay(1500) // let the connection settle before calling the API
                            val repository = EventRepository(applicationContext)
                            repository.syncPendingActions()
                            FirebaseAuth.getInstance().currentUser?.uid?.let { repository.refreshSchedule(it) }
                        } catch (e: Exception) {
                            Log.e(TAG, "Background sync failed", e)
                        }
                    }
                }

                override fun onLost(network: Network) {
                    Log.i(TAG, "Network lost - app is now in offline mode (Room cache)")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Could not register network callback", e)
        }
    }

    companion object {
        private const val TAG = "TWS-App"
    }
}