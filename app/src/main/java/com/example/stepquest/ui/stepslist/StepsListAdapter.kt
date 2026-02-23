package com.example.stepquest.ui.stepslist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.stepquest.R
import com.example.stepquest.databinding.ItemStepsListBinding
import com.example.stepquest.domain.model.StepsListItem
import com.google.android.material.color.MaterialColors

class StepsListAdapter(
    private val items: List<StepsListItem>
) : RecyclerView.Adapter<StepsListAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemStepsListBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStepsListBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.textLabel.text = item.label
        holder.binding.textSteps.text = item.stepsText

        val color = if (item.highlightGreen) {
            ContextCompat.getColor(holder.itemView.context, R.color.goal_met_green)
        } else {
            MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnSurface)
        }
        holder.binding.textSteps.setTextColor(color)
    }

    override fun getItemCount(): Int = items.size
}
