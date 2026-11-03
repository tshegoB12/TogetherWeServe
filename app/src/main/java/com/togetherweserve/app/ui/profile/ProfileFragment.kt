package com.togetherweserve.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.togetherweserve.app.R
import com.togetherweserve.app.data.repository.EventRepository
import com.togetherweserve.app.databinding.FragmentProfileBinding
import com.togetherweserve.app.utils.ImpactCalculator
import com.togetherweserve.app.utils.SessionManager
import kotlinx.coroutines.launch

/** Profile and impact dashboard. Impact is calculated from confirmed registrations. */
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false); return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val user = FirebaseAuth.getInstance().currentUser
        val session = SessionManager(requireContext())
        binding.tvProfileName.text = user?.displayName ?: getString(R.string.volunteer)
        binding.tvProfileEmail.text = user?.email ?: ""
        binding.tvFavorites.text = getString(R.string.saved_events_count, session.favoriteCount())
        viewLifecycleOwner.lifecycleScope.launch {
            EventRepository(requireContext()).observeSchedule().collect { list ->
                val confirmed = ImpactCalculator.confirmedCount(list.map { it.status })
                binding.tvImpact.text = getString(
                    R.string.impact_summary, confirmed, ImpactCalculator.estimatedHours(confirmed)
                )
            }
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
