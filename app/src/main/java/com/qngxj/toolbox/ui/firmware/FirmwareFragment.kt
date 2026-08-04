package com.qngxj.toolbox.ui.firmware

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.qngxj.toolbox.R

/** 固件查询（V1.0 纯本地 UI，无联网） */
class FirmwareFragment : Fragment() {

    private lateinit var rv: RecyclerView
    private lateinit var chips: ChipGroup
    private lateinit var search: android.widget.EditText

    data class Fw(val model: String, val version: String, val android: String, val cat: String, val date: String)

    private val data: List<Fw> = listOf(
        Fw("Pixel 8 Pro", "AP1A.240505.005", "Android 14", "stable", "2024-05-05"),
        Fw("Pixel 8", "AP1A.240505.005", "Android 14", "stable", "2024-05-05"),
        Fw("Pixel 7 Pro", "UQ1A.240205.004", "Android 14", "stable", "2024-02-05"),
        Fw("Samsung S24 Ultra", "OneUI 6.1 / 9981.1", "Android 14", "custom", "2024-04-09"),
        Fw("Samsung S23", "OneUI 6.0", "Android 14", "stable", "2023-12-18"),
        Fw("Xiaomi 14", "HyperOS 1.0.5", "Android 14", "custom", "2024-03-20"),
        Fw("Xiaomi 13 Pro", "MIUI 14.0.23", "Android 13", "stable", "2023-09-10"),
        Fw("OPPO Find X7", "ColorOS 14.0", "Android 14", "custom", "2024-01-12"),
        Fw("vivo X100 Pro", "OriginOS 4.0", "Android 14", "custom", "2023-11-21"),
        Fw("HUAWEI Mate 60 Pro", "HarmonyOS 4.2", "HarmonyOS", "custom", "2024-04-08"),
        Fw("OnePlus 12", "OxygenOS 14.0.1", "Android 14", "stable", "2024-01-23"),
        Fw("Pixel 8 Pro (Beta)", "APB1.240405.012", "Android 15 DP", "beta", "2024-04-10"),
        Fw("Pixel 7 (Beta)", "APB1.240405.006", "Android 15 DP", "beta", "2024-04-10"),
        Fw("Generic AOSP", "mainline-eng", "Android 15", "dev", "2024-05-01"),
        Fw("Generic AOSP", "mainline-userdebug", "Android 14", "dev", "2024-03-01"),
        Fw("Realme GT5 Pro", "RealmeUI 5.0", "Android 14", "custom", "2024-02-15")
    )

    private val cats = listOf("all" to R.string.firmware_cat_all,
        "stable" to R.string.firmware_cat_stable,
        "beta" to R.string.firmware_cat_beta,
        "dev" to R.string.firmware_cat_dev,
        "custom" to R.string.firmware_cat_custom)

    private var currentCat = "all"
    private var query = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_firmware, container, false)
        rv = v.findViewById(R.id.rv)
        chips = v.findViewById(R.id.chips)
        search = v.findViewById(R.id.search)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = view.context
        for ((key, label) in cats) {
            val chip = Chip(ctx)
            chip.text = getString(label)
            chip.isCheckable = true
            chip.isChecked = key == "all"
            chip.setOnClickListener { currentCat = key; refresh() }
            chips.addView(chip)
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { query = s?.toString() ?: ""; refresh() }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        rv.layoutManager = LinearLayoutManager(ctx)
        refresh()
    }

    private fun catLabel(cat: String): String {
        val res = cats.firstOrNull { it.first == cat }?.second ?: R.string.firmware_cat_all
        return getString(res)
    }

    private fun catColor(cat: String): Int {
        return when (cat) {
            "stable" -> 0xFF22C55E.toInt()
            "beta" -> 0xFFF59E0B.toInt()
            "dev" -> 0xFF2E7DFF.toInt()
            "custom" -> 0xFF8B5CF6.toInt()
            else -> 0xFF6B7280.toInt()
        }
    }

    private fun refresh() {
        val list = data.filter { fw ->
            (currentCat == "all" || fw.cat == currentCat) &&
                (query.isBlank() || listOf(fw.model, fw.version, fw.android, fw.cat)
                    .any { it.lowercase().contains(query.lowercase()) })
        }
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_firmware, parent, false)
                return object : RecyclerView.ViewHolder(v) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val fw = list[position]
                holder.itemView.findViewById<TextView>(R.id.tv_model).text = fw.model
                holder.itemView.findViewById<TextView>(R.id.tv_version).text = "${fw.version} · ${fw.android}"
                holder.itemView.findViewById<TextView>(R.id.tv_date).text = fw.date
                val catTv = holder.itemView.findViewById<TextView>(R.id.tv_cat)
                catTv.text = catLabel(fw.cat)
                catTv.setBackgroundColor(catColor(fw.cat))
                holder.itemView.setOnClickListener {
                    val i = Intent(holder.itemView.context, FirmwareDetailActivity::class.java)
                    i.putExtra("model", fw.model)
                    i.putExtra("version", fw.version)
                    i.putExtra("android", fw.android)
                    i.putExtra("cat", catLabel(fw.cat))
                    i.putExtra("date", fw.date)
                    holder.itemView.context.startActivity(i)
                }
            }

            override fun getItemCount(): Int = list.size
        }
    }
}
