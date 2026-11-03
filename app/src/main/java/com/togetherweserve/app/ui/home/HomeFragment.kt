package com.togetherweserve.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.togetherweserve.app.data.local.EventEntity
import com.togetherweserve.app.data.repository.EventRepository
import com.togetherweserve.app.databinding.FragmentHomeBinding
import com.togetherweserve.app.ui.eventdetail.EventDetailActivity
import com.togetherweserve.app.utils.EventFilter
import kotlinx.coroutines.launch

/**
 * FR5 - Event discovery: searchable, filterable feed of nearby events
 * grouped by cause, matching the "Home / Event Feed" wireframe.
 * Reads through EventRepository so the feed still shows the last-cached
 * events when the device is offline (FR8).
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: EventRepository
    private lateinit var adapter: EventAdapter

    private val causes = listOf("Environment", "Education", "Health", "Community")
    private var latestEvents: List<EventEntity> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = EventRepository(requireContext())

        adapter = EventAdapter(
            onJoinClick = { event -> openEventDetail(event) },
            onItemClick = { event -> openEventDetail(event) }
        )
        // Without a LayoutManager a RecyclerView draws nothing, even when the adapter has items.
        binding.recyclerEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerEvents.adapter = adapter

        causes.forEach { cause ->
            val chip = Chip(requireContext()).apply {
                text = cause
                isCheckable = true
            }
            binding.chipGroupCauses.addView(chip)
        }

        binding.swipeRefresh.setOnRefreshListener { refresh() }

        // Tied to the fragment's view lifecycle: the flow is only collected
        // while the view is at least STARTED, and is automatically cancelled
        // in onDestroyView, so it can never touch `binding` after it's torn down.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeEvents().collect { events ->
                    latestEvents = events
                    showFilteredEvents(filterEvents(events))
                }
            }
        }

        binding.inputSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                showFilteredEvents(filterEvents(latestEvents))
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        refresh()
    }

    private fun refresh() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.refreshEvents()
            if (_binding != null) binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun showFilteredEvents(events: List<EventEntity>) {
        adapter.submitList(events)
        binding.tvEmptyEvents.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun filterEvents(events: List<EventEntity>): List<EventEntity> {
        val query = binding.inputSearch.text?.toString().orEmpty()
        val selectedCauses = binding.chipGroupCauses.checkedChipIds
            .mapNotNull { id -> binding.chipGroupCauses.findViewById<Chip>(id)?.text?.toString() }
        return EventFilter.filter(events, query, selectedCauses)
    }

    private fun openEventDetail(event: EventEntity) {
        val intent = Intent(requireContext(), EventDetailActivity::class.java)
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.eventId)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
