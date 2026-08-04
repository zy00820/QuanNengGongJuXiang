package com.qngxj.toolbox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.qngxj.toolbox.R

data class HardwareCard(val title: String, val rows: List<Pair<String, String>>)

class HardwareCardAdapter(private val cards: List<HardwareCard>) :
    RecyclerView.Adapter<HardwareCardAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tv_title)
        val rows: LinearLayout = itemView.findViewById(R.id.rows)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_hardware_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val card = cards[position]
        holder.title.text = card.title
        holder.rows.removeAllViews()
        for ((k, v) in card.rows) {
            val row = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.item_info_row, holder.rows, false) as LinearLayout
            row.findViewById<TextView>(R.id.tv_key).text = k
            row.findViewById<TextView>(R.id.tv_value).text = v
            holder.rows.addView(row)
        }
    }

    override fun getItemCount(): Int = cards.size
}
