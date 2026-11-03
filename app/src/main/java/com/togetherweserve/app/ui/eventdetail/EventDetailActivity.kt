package com.togetherweserve.app.ui.eventdetail

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.togetherweserve.app.utils.SessionManager
import com.togetherweserve.app.R
import com.togetherweserve.app.data.local.EventEntity
import com.togetherweserve.app.data.repository.EventRepository
import com.togetherweserve.app.databinding.ActivityEventDetailBinding
import com.togetherweserve.app.ui.group.GroupSignupActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * FR5 / FR6 / FR7 - shows full event info and lets the user register
 * individually or hand off to Group/Team Sign-up (the app's key
 * differentiator, per the Planning & Design document).
 */
class EventDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailBinding
    private lateinit var repository: EventRepository
    private var currentEvent: EventEntity? = null
    private var selectedMode: String = "individual"
    private var groupCode: String? = null
    private lateinit var session: SessionManager
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val groupSignupLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val code = result.data?.getStringExtra(GroupSignupActivity.RESULT_GROUP_CODE)
        if (code != null) {
            selectedMode = "group"
            groupCode = code
            binding.tvInviteCode.visibility = View.VISIBLE
            binding.tvInviteCode.text = getString(R.string.invite_by_code_format, code)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = EventRepository(this)
        session = SessionManager(this)

        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: run { finish(); return }

        // Do not leave a blank detail screen when the API/cache cannot find an event.
        // The previous code silently ignored a null result, making a tap look like it did nothing.
        binding.progressBar.visibility = View.VISIBLE
        binding.btnConfirmSignup.isEnabled = false
        appScope.launch {
            try {
                val event = repository.getEvent(eventId)
                currentEvent = event
                if (event != null) {
                    bind(event)
                } else {
                    binding.tvTitle.text = getString(R.string.event_unavailable_title)
                    binding.tvDescription.text = getString(R.string.event_unavailable_message)
                    binding.btnConfirmSignup.isEnabled = false
                    Snackbar.make(binding.root, R.string.event_unavailable_message, Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not load event $eventId", e)
                binding.tvTitle.text = getString(R.string.event_unavailable_title)
                binding.tvDescription.text = getString(R.string.event_unavailable_message)
                Snackbar.make(binding.root, R.string.event_unavailable_message, Snackbar.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnConfirmSignup.isEnabled = currentEvent != null
            }
        }

        binding.btnJoinIndividual.setOnClickListener {
            selectedMode = "individual"
            groupCode = null
            binding.tvInviteCode.visibility = View.GONE
        }

        binding.btnJoinGroup.setOnClickListener {
            val intent = Intent(this, GroupSignupActivity::class.java)
            intent.putExtra(GroupSignupActivity.EXTRA_EVENT_ID, eventId)
            groupSignupLauncher.launch(intent)
        }

        binding.btnConfirmSignup.setOnClickListener {
            confirmSignup()
        }

        binding.btnFavorite.setOnClickListener {
            val id = currentEvent?.eventId ?: return@setOnClickListener
            val newValue = !session.isFavorite(id)
            session.setFavorite(id, newValue)
            Log.i(TAG, "Favourite toggled for $id -> $newValue")
            updateFavoriteIcon()
            Snackbar.make(binding.root, if (newValue) getString(R.string.added_to_favorites) else getString(R.string.removed_from_favorites), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun bind(event: EventEntity) {
        binding.tvTitle.text = event.title
        val df = SimpleDateFormat("EEE, dd MMM · HH:mm", Locale.getDefault())
        binding.tvDateTime.text = df.format(event.dateTime)
        binding.tvLocationOrganiser.text = getString(R.string.hosted_by_format, event.location, event.organiserName)
        binding.tvDescription.text = event.description
        binding.tvSpotsLeft.text = getString(
            R.string.spots_left_format, event.volunteersGoing, event.slotsAvailable
        )
        updateFavoriteIcon()
    }

    private fun updateFavoriteIcon() {
        binding.btnFavorite.text = if (session.isFavorite(currentEvent?.eventId.orEmpty()))
            getString(R.string.saved_event) else getString(R.string.save_event)
    }

    private fun confirmSignup() {
        val event = currentEvent ?: return
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Snackbar.make(binding.root, getString(R.string.registration_requires_login), Snackbar.LENGTH_LONG).show()
            return
        }
        binding.btnConfirmSignup.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        appScope.launch {
            try {
                val wentToServer = repository.joinEvent(event.eventId, event.title, event.dateTime, groupCode)
                // Refresh server-side schedule when online; Room emits the local registration immediately.
                if (wentToServer) repository.refreshSchedule(userId)
                val message = if (wentToServer) getString(R.string.registration_success_schedule)
                              else getString(R.string.registration_saved_offline)
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Registration failed for event ${event.eventId}", e)
                Snackbar.make(binding.root, getString(R.string.registration_failed), Snackbar.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnConfirmSignup.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        appScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_EVENT_ID = "extra_event_id"
        private const val TAG = "TWS-EventDetail"
    }
}
