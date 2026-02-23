package com.example.stepquest.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.stepquest.StepQuestApplication
import com.example.stepquest.R
import com.example.stepquest.databinding.FragmentFirstBinding
import com.example.stepquest.domain.model.DashboardError
import com.example.stepquest.domain.model.DashboardRow
import com.example.stepquest.domain.model.DashboardState
import com.example.stepquest.widget.StepsWidgetProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DashboardFragment : Fragment(), MenuProvider {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory(StepQuestApplication.get(requireContext()))
    }

    private val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)

    private val permissionLauncher =
        registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { granted ->
            viewModel.onHcPermissionResult(stepsPermission in granted)
        }

    private val activityRecognitionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            viewModel.onRecordingApiPermissionResult(granted)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(this, viewLifecycleOwner)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.performFullRefresh()
        }

        binding.buttonGrantPermission.setOnClickListener {
            permissionLauncher.launch(setOf(stepsPermission))
        }

        binding.rowDay.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        binding.rowLast7.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_WeeklySteps)
        }

        binding.rowMonth.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_MonthlySteps)
        }

        viewModel.setupRecordingApi(
            hasActivityRecognitionPermission = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED,
            onNeedPermission = {
                activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        )

        viewModel.checkAndLoadSteps()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state -> renderState(state) }
                }
                launch {
                    while (true) {
                        delay(1_000)
                        viewModel.autoRefreshToday()
                    }
                }
            }
        }
    }

    private fun renderState(state: DashboardState) {
        binding.swipeRefresh.isRefreshing = state.isRefreshing

        updateRow(binding.countDay, binding.barDay, state.today)
        updateRow(binding.countLast7, binding.barLast7, state.last7)
        updateRow(binding.countMonth, binding.barMonth, state.month)
        updateRow(binding.countLast30, binding.barLast30, state.last30)
        updateRow(binding.countYear, binding.barYear, state.year)

        binding.buttonGrantPermission.visibility =
            if (state.showGrantPermission) View.VISIBLE else View.GONE

        when (state.error) {
            DashboardError.PERMISSION_DENIED -> {
                binding.textStatus.text = getString(R.string.permission_denied)
                binding.textStatus.visibility = View.VISIBLE
            }
            DashboardError.READING_ERROR -> {
                binding.textStatus.text = getString(R.string.error_reading_steps)
                binding.textStatus.visibility = View.VISIBLE
            }
            null -> binding.textStatus.visibility = View.GONE
        }

        if (!state.isRefreshing) {
            StepsWidgetProvider.requestUpdate(requireContext())
        }
    }

    private fun updateRow(
        countView: android.widget.TextView,
        barView: com.google.android.material.progressindicator.LinearProgressIndicator,
        row: DashboardRow
    ) {
        countView.text = "%,d · %d%%".format(row.steps, row.percent)
        barView.setProgressCompat(row.percent.coerceAtMost(100), true)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_first, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_set_goal -> {
                showGoalDialog()
                true
            }
            else -> false
        }
    }

    private fun showGoalDialog() {
        val editText = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(viewModel.getYearlyGoal().toString())
            hint = getString(R.string.goal_dialog_hint)
            selectAll()
        }
        val container = FrameLayout(requireContext()).apply {
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, 0)
            addView(editText)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.goal_dialog_title))
            .setMessage(getString(R.string.goal_dialog_message))
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val value = editText.text.toString().toLongOrNull()
                if (value != null && value > 0) {
                    viewModel.setYearlyGoal(value)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
