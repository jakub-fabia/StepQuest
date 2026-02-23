package com.example.stepquest.ui.dailysteps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stepquest.StepQuestApplication
import com.example.stepquest.R
import com.example.stepquest.databinding.FragmentSecondBinding
import com.example.stepquest.domain.model.DailyStepsListState
import com.example.stepquest.widget.StepsWidgetProvider
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class DailyStepsFragment : Fragment(), MenuProvider {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DailyStepsViewModel by viewModels {
        DailyStepsViewModel.Factory(StepQuestApplication.get(requireContext()))
    }

    private val csvPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.importCsv(uri)
        }

    private val csvExportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri != null) viewModel.exportCsv(uri)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(this, viewLifecycleOwner)

        binding.recyclerDailySteps.layoutManager = LinearLayoutManager(requireContext())

        viewModel.loadDailySteps()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state -> renderState(state) }
                }
                launch {
                    viewModel.importEvents.collect { event ->
                        when (event) {
                            is ImportEvent.Success -> {
                                Snackbar.make(
                                    binding.root,
                                    getString(R.string.csv_import_success, event.count),
                                    Snackbar.LENGTH_SHORT
                                ).show()
                                StepsWidgetProvider.requestUpdate(requireContext())
                            }
                            ImportEvent.NoData -> {
                                Snackbar.make(binding.root, R.string.csv_import_no_data, Snackbar.LENGTH_SHORT).show()
                            }
                            ImportEvent.Error -> {
                                Snackbar.make(binding.root, R.string.csv_import_error, Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                launch {
                    viewModel.exportEvents.collect { event ->
                        when (event) {
                            is ExportEvent.Success -> {
                                Snackbar.make(
                                    binding.root,
                                    getString(R.string.csv_export_success, event.count),
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                            ExportEvent.NoData -> {
                                Snackbar.make(binding.root, R.string.csv_export_no_data, Snackbar.LENGTH_SHORT).show()
                            }
                            ExportEvent.Error -> {
                                Snackbar.make(binding.root, R.string.csv_export_error, Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderState(state: DailyStepsListState) {
        when (state) {
            is DailyStepsListState.Loading -> {
                binding.textStatus.text = getString(R.string.loading_steps)
                binding.textStatus.visibility = View.VISIBLE
            }
            is DailyStepsListState.Success -> {
                binding.textStatus.visibility = View.GONE
                binding.recyclerDailySteps.adapter = DailyStepsAdapter(state.items, state.dailyGoal)
            }
            is DailyStepsListState.Empty -> {
                binding.textStatus.text = getString(R.string.no_step_data)
                binding.textStatus.visibility = View.VISIBLE
            }
            is DailyStepsListState.Error -> {
                binding.textStatus.text = getString(R.string.error_reading_steps)
                binding.textStatus.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_second, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_import_csv -> {
                csvPickerLauncher.launch(arrayOf("text/*"))
                true
            }
            R.id.action_export_csv -> {
                csvExportLauncher.launch("stepquest-export.csv")
                true
            }
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
