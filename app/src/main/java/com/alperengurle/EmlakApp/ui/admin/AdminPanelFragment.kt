package com.alperengurle.EmlakApp.ui.admin

// AdminPanelFragment.kt - app/src/main/java/com/alperengurle/EmlakApp/ui/admin/AdminPanelFragment.kt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.alperengurle.EmlakApp.databinding.FragmentAdminPanelBinding
import com.alperengurle.EmlakApp.viewmodel.AdminPanelViewModel
import com.google.android.material.tabs.TabLayout


class AdminPanelFragment : Fragment() {
    private var _binding: FragmentAdminPanelBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminPanelViewModel by viewModels()
    private lateinit var pendingListingsAdapter: AdminListingsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminPanelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
        loadPendingListings()
    }

    private fun setupUI() {
        binding.apply {
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            // Tab Layout Setup
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    when(tab?.position) {
                        0 -> loadPendingListings()
                        1 -> loadApprovedListings()
                        2 -> loadRejectedListings()
                    }
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })

            // RecyclerView Setup
            pendingListingsAdapter = AdminListingsAdapter(
                onApprove = { listingId -> viewModel.approveListing(listingId) },
                onReject = { listingId -> viewModel.rejectListing(listingId) }
            )

            recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = pendingListingsAdapter
            }
        }
    }

    private fun observeViewModel() {
        viewModel.listings.observe(viewLifecycleOwner) { listings ->
            listings?.let {
                pendingListingsAdapter.submitList(it.toMutableList())
                binding.emptyView.visibility = if(it.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if(isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadPendingListings() {
        viewModel.loadListings("pending")
    }

    private fun loadApprovedListings() {
        viewModel.loadListings("approved")
    }

    private fun loadRejectedListings() {
        viewModel.loadListings("rejected")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}