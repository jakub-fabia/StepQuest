package com.example.stepquest.ui.dailysteps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.stepquest.R
import com.example.stepquest.databinding.ItemDailyStepsBinding
import com.example.stepquest.domain.model.DailySteps
import com.google.android.material.color.MaterialColors
import java.time.format.DateTimeFormatter

class DailyStepsAdapter(
    private val items: List<DailySteps>,
    private val dailyGoal: Long = 0
) : RecyclerView.Adapter<DailyStepsAdapter.ViewHolder>() {

    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

    class ViewHolder(val binding: ItemDailyStepsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDailyStepsBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.textDate.text = item.date.format(dateFormatter)
        holder.binding.textSteps.text = "%,d".format(item.steps)

        val color = if (dailyGoal > 0 && item.steps >= dailyGoal) {
            ContextCompat.getColor(holder.itemView.context, R.color.goal_met_green)
        } else {
            MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnSurface)
        }
        holder.binding.textSteps.setTextColor(color)
    }

    override fun getItemCount(): Int = items.size
}
