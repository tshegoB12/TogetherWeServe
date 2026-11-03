package com.togetherweserve.app.ui.organiser

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.togetherweserve.app.data.remote.RetrofitClient
import com.togetherweserve.app.data.remote.models.CreateEventRequest
import com.togetherweserve.app.databinding.ActivityCreateEventBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Calendar

/** FR11 - Organiser event creation, matching the "Create Event (NPO)" wireframe. */
class CreateEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateEventBinding
    private var selectedDateTime: Long = System.currentTimeMillis()
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.inputDateTime.setOnClickListener { showDatePicker() }

        binding.btnPublish.setOnClickListener { publish() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            selectedDateTime = cal.timeInMillis
            binding.inputDateTime.setText(
                android.text.format.DateFormat.format("EEE, dd MMM yyyy", cal).toString()
            )
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun publish() {
        val title = binding.inputTitle.text?.toString()?.trim().orEmpty()
        val cause = binding.inputCause.text?.toString()?.trim().orEmpty()
        val location = binding.inputLocation.text?.toString()?.trim().orEmpty()
        val description = binding.inputDescription.text?.toString()?.trim().orEmpty()
        val slots = binding.inputSlots.text?.toString()?.toIntOrNull() ?: 0

        if (title.isEmpty() || cause.isEmpty() || location.isEmpty() || slots <= 0) {
            Log.w(TAG, "Publish blocked: required fields missing")
            Snackbar.make(binding.root, getString(com.togetherweserve.app.R.string.error_required_field), Snackbar.LENGTH_LONG).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        appScope.launch {
            try {
                RetrofitClient.api.createEvent(
                    CreateEventRequest(title, cause, description, selectedDateTime, location, slots)
                )
                Log.i(TAG, "Event published via REST API (REST API will push FCM alert)")
                binding.progressBar.visibility = View.GONE
                Snackbar.make(binding.root, "Event published", Snackbar.LENGTH_LONG).show()
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Publishing event failed", e)
                binding.progressBar.visibility = View.GONE
                Snackbar.make(binding.root, "Could not publish: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        appScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "TWS-CreateEvent"
    }
}