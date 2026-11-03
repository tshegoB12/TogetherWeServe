package com.togetherweserve.app.ui.group

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.togetherweserve.app.data.repository.EventRepository
import com.togetherweserve.app.databinding.ActivityGroupSignupBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * FR7 - Group/Team sign-up: create a named group (server generates a
 * shareable invite code) or enter an existing code to join a group already
 * registered for this event. This is TogetherWeServe's key differentiator
 * from the three researched apps (POINT, SignUp.com, Eventvolunteers).
 */
class GroupSignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupSignupBinding
    private lateinit var repository: EventRepository
    private lateinit var eventId: String
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupSignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = EventRepository(this)
        eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: run { finish(); return }

        binding.btnCreateGroup.setOnClickListener {
            val name = binding.inputGroupName.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                Snackbar.make(binding.root, "Enter a group name", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.progressBar.visibility = View.VISIBLE
            appScope.launch {
                try {
                    val response = repository.createGroup(eventId, name)
                    finishWithCode(response.groupCode)
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, "Could not create group - check connection", Snackbar.LENGTH_LONG).show()
                }
            }
        }

        binding.btnJoinGroup.setOnClickListener {
            val code = binding.inputInviteCode.text?.toString()?.trim().orEmpty()
            if (code.isEmpty()) {
                Snackbar.make(binding.root, "Enter an invite code", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            finishWithCode(code)
        }
    }

    private fun finishWithCode(code: String) {
        val result = Intent().putExtra(RESULT_GROUP_CODE, code)
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    override fun onDestroy() {
        appScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val RESULT_GROUP_CODE = "result_group_code"
    }
}
