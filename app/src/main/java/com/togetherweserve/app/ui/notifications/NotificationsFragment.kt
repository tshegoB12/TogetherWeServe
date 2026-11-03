package com.togetherweserve.app.ui.notifications

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.togetherweserve.app.R
import com.togetherweserve.app.databinding.FragmentNotificationsBinding
import com.togetherweserve.app.ui.eventdetail.EventDetailActivity
import com.togetherweserve.app.utils.NotificationStore

/**
 * Real-time notifications screen: shows every push alert received through
 * Firebase Cloud Messaging, newest first, and updates live when a new one arrives.
 */
class NotificationsFragment : Fragment() {
    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var store: NotificationStore

    private val adapter = NotificationAdapter { item ->
        Log.d(TAG, "Notification tapped: ${item.title}")
        item.eventId?.let {
            startActivity(
                Intent(requireContext(), EventDetailActivity::class.java)
                    .putExtra(EventDetailActivity.EXTRA_EVENT_ID, it)
            )
        }
    }

    // Keep a strong reference - SharedPreferences only holds listeners weakly.
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == NotificationStore.KEY && _binding != null) render()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = NotificationStore(requireContext())
        binding.tvNotificationTitle.text = getString(R.string.notification_screen_title)
        binding.tvNotificationBody.text = getString(R.string.notifications_body)
        binding.recyclerNotifications.adapter = adapter
        binding.btnClearNotifications.setOnClickListener {
            store.clear()
            render()
        }
        store.registerListener(prefsListener)
        render()
    }

    private fun render() {
        val items = store.all()
        adapter.submitList(items)
        binding.tvEmptyNotifications.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.btnClearNotifications.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        store.unregisterListener(prefsListener)
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "TWS-Notifications"
    }
}
