package com.example.startwatch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.startwatch.databinding.ItemIconBinding

class IconPickerAdapter(
    private val icons: List<Pair<String, Int>>,
    private var selectedIconName: String,
    private val onIconSelected: (String, Int) -> Unit
) : RecyclerView.Adapter<IconPickerAdapter.IconViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val binding = ItemIconBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IconViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(icons[position])
    }

    override fun getItemCount(): Int = icons.size

    inner class IconViewHolder(private val binding: ItemIconBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(iconPair: Pair<String, Int>) {
            val (name, resId) = iconPair
            binding.imageIcon.setImageResource(resId)

            if (name == selectedIconName) {
                binding.imageIcon.setBackgroundResource(R.drawable.bg_icon_selected)
            } else {
                binding.imageIcon.setBackgroundResource(android.R.color.transparent)
            }

            binding.root.setOnClickListener {
                val previousSelected = selectedIconName
                selectedIconName = name
                // Notify only the changed items
                val oldIndex = icons.indexOfFirst { it.first == previousSelected }
                val newIndex = icons.indexOfFirst { it.first == selectedIconName }
                if (oldIndex != -1) notifyItemChanged(oldIndex)
                if (newIndex != -1) notifyItemChanged(newIndex)
                
                onIconSelected(name, resId)
            }
        }
    }
}
