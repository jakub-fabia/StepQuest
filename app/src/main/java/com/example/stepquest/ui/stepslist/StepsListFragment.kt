package com.example.stepquest.ui.stepslist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stepquest.StepQuestApplication
import com.example.stepquest.R
import com.example.stepquest.databinding.FragmentStepsListBinding
import com.example.stepquest.domain.model.StepsListState
import kotlinx.coroutines.launch

class StepsListFragment : Fragment() {

    private var _binding: FragmentStepsListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StepsListViewModel by viewModels {
        StepsListViewModel.Factory(StepQuestApplication.get(requireContext()))
    }

    companion object {
        const val ARG_MODE = "mode"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStepsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerSteps.layoutManager = LinearLayoutManager(requireContext())

        val mode = arguments?.getString(ARG_MODE) ?: StepsListViewModel.MODE_WEEKLY
        viewModel.loadData(mode)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state -> renderState(state) }
            }
        }
    }

    private fun renderState(state: StepsListState) {
        when (state) {
            is StepsListState.Loading -> {
                binding.textStatus.text = getString(R.string.loading_steps)
                binding.textStatus.visibility = View.VISIBLE
            }
            is StepsListState.Success -> {
                binding.textStatus.visibility = View.GONE
                binding.recyclerSteps.adapter = StepsListAdapter(state.items)
            }
            is StepsListState.Empty -> {
                binding.textStatus.text = getString(R.string.no_step_data)
                binding.textStatus.visibility = View.VISIBLE
            }
            is StepsListState.Error -> {
                binding.textStatus.text = getString(R.string.error_reading_steps)
                binding.textStatus.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
