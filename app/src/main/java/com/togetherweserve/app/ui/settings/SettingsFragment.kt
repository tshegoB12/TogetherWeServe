package com.togetherweserve.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.togetherweserve.app.R
import com.togetherweserve.app.auth.LoginActivity
import com.togetherweserve.app.data.remote.RetrofitClient
import com.togetherweserve.app.data.remote.models.SettingsUpdateRequest
import com.togetherweserve.app.databinding.FragmentSettingsBinding
import com.togetherweserve.app.utils.ConnectivityObserver
import com.togetherweserve.app.utils.NotificationTopics
import com.togetherweserve.app.utils.SupportedLanguages
import com.togetherweserve.app.utils.SessionManager
import kotlinx.coroutines.launch

/** Settings screen: profile, notifications, language, offline sync and security. */
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        session = SessionManager(requireContext())

        binding.rowProfile.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.profile_information))
                .setMessage("${user?.displayName ?: "-"}\n${user?.email ?: "-"}\n\n${getString(R.string.saved_events_count, session.favoriteCount())}")
                .setPositiveButton(R.string.close, null).show()
        }

        binding.rowNotifications.setOnClickListener {
            val items = arrayOf(getString(R.string.event_reminders), getString(R.string.chat_messages))
            val checked = booleanArrayOf(session.eventRemindersEnabled, session.chatMessagesEnabled)
            AlertDialog.Builder(requireContext()).setTitle(getString(R.string.notification_preferences))
                .setMultiChoiceItems(items, checked) { _, which, isChecked ->
                    if (which == 0) session.eventRemindersEnabled = isChecked else session.chatMessagesEnabled = isChecked
                }.setPositiveButton(R.string.done) { _, _ ->
                    Log.i(TAG, "Notification prefs saved: events=${session.eventRemindersEnabled}, chat=${session.chatMessagesEnabled}")
                    // Make the switch real: (un)subscribe from the FCM event-alert topic.
                    NotificationTopics.apply(session.eventRemindersEnabled)
                    pushSettingsToServer()
                }.show()
        }

        binding.rowLanguage.setOnClickListener {
            val languages = SupportedLanguages.all.map { it.label }.toTypedArray()
            val codes = SupportedLanguages.all.map { it.code }
            val current = SupportedLanguages.indexOf(session.language)
            AlertDialog.Builder(requireContext()).setTitle(getString(R.string.choose_language))
                .setSingleChoiceItems(languages, current) { dialog, which ->
                    session.language = codes[which]
                    Log.i(TAG, "Language changed to ${codes[which]}")
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(codes[which]))
                    dialog.dismiss()
                    requireActivity().recreate()
                }.show()
        }

        binding.rowOfflineData.setOnClickListener {
            AlertDialog.Builder(requireContext()).setTitle(getString(R.string.offline_data_sync))
                .setMessage(getString(R.string.offline_explanation)).setPositiveButton(R.string.close, null).show()
        }

        binding.rowPrivacy.setOnClickListener {
            AlertDialog.Builder(requireContext()).setTitle(getString(R.string.privacy_security))
                .setMessage(getString(R.string.privacy_explanation)).setPositiveButton(R.string.close, null).show()
        }

        binding.rowOrganiser.setOnClickListener {
            // Organiser mode reveals the "Create Event" button. The REST API still verifies the
            // Firestore user role, so this switch cannot be used to bypass server security.
            // NOTE: AlertDialog hides list/checkbox items whenever setMessage() is used,
            // so the explanation and the switch are placed together in a custom view.
            val ctx = requireContext()
            val pad = (20 * resources.displayMetrics.density).toInt()
            val explanation = android.widget.TextView(ctx).apply {
                text = getString(R.string.organiser_mode_explanation)
                setPadding(0, 0, 0, pad)
            }
            val organiserSwitch = com.google.android.material.switchmaterial.SwitchMaterial(ctx).apply {
                text = getString(R.string.organiser_mode)
                isChecked = session.role == "organiser"
            }
            val content = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(pad, pad, pad, 0)
                addView(explanation)
                addView(organiserSwitch)
            }
            AlertDialog.Builder(ctx).setTitle(getString(R.string.organiser_mode))
                .setView(content)
                .setPositiveButton(R.string.done) { _, _ ->
                    session.role = if (organiserSwitch.isChecked) "organiser" else "volunteer"
                    Log.i(TAG, "Organiser mode set to ${session.role}")
                    (requireActivity() as? com.togetherweserve.app.MainActivity)?.refreshOrganiserButton()
                    Snackbar.make(binding.root, R.string.settings_saved, Snackbar.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.close, null)
                .show()
        }

        binding.rowLogout.setOnClickListener {
            Log.i(TAG, "User logged out")
            FirebaseAuth.getInstance().signOut(); session.clear()
            startActivity(Intent(requireContext(), LoginActivity::class.java)); requireActivity().finish()
        }
    }

    private fun pushSettingsToServer() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (!ConnectivityObserver.isOnline(requireContext())) {
            Snackbar.make(binding.root, R.string.offline_settings_saved, Snackbar.LENGTH_SHORT).show(); return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                RetrofitClient.api.updateSettings(userId, SettingsUpdateRequest(session.language, session.eventRemindersEnabled, session.chatMessagesEnabled))
                Log.i(TAG, "Settings synced to REST API")
                Snackbar.make(binding.root, R.string.settings_saved, Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Settings sync failed", e)
                Snackbar.make(binding.root, R.string.settings_sync_failed, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        private const val TAG = "TWS-Settings"
    }
}