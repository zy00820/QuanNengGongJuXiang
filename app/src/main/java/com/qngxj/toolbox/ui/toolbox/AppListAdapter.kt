package com.qngxj.toolbox.ui.toolbox

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.qngxj.toolbox.R
import com.qngxj.toolbox.util.DeviceUtils

class AppListAdapter(
    private val ctx: Context,
    private val extractMode: Boolean,
    private val onClick: (DeviceUtils.AppItem) -> Unit
) : RecyclerView.Adapter<AppListAdapter.VH>() {

    private var list: List<DeviceUtils.AppItem> = emptyList()
    private var full: List<DeviceUtils.AppItem> = emptyList()

    fun submit(data: List<DeviceUtils.AppItem>) {
        full = data
        list = data
        notifyDataSetChanged()
    }

    fun filter(q: String) {
        val query = q.trim().lowercase()
        list = if (query.isEmpty()) full else full.filter {
            it.name.lowercase().contains(query) || it.pkg.lowercase().contains(query)
        }
        notifyDataSetChanged()
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.iv_icon)
        val name: TextView = itemView.findViewById(R.id.tv_name)
        val pkg: TextView = itemView.findViewById(R.id.tv_pkg)
        val ver: TextView = itemView.findViewById(R.id.tv_ver)
        val btn: MaterialButton = itemView.findViewById(R.id.btn_action)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return VH(v)
    }

    private fun icon(pkg: String): android.graphics.drawable.Drawable? {
        return try { ctx.packageManager.getApplicationIcon(pkg) } catch (e: Exception) { null }
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]
        holder.name.text = item.name
        holder.pkg.text = (if (item.systemApp) "[系统] " else "") + item.pkg
        holder.ver.text = "v${item.versionName} (${item.versionCode}) · ${humanSize(item.sizeBytes)}"
        holder.icon.setImageDrawable(icon(item.pkg))
        holder.btn.text = if (extractMode) ctx.getString(R.string.extract_apk) else ctx.getString(R.string.view_detail)
        holder.itemView.setOnClickListener { onClick(item) }
        holder.btn.setOnClickListener { onClick(item) }
    }

    private fun humanSize(b: Long): String {
        val mb = b / (1024.0 * 1024.0)
        return if (mb >= 1024) String.format("%.1f GB", mb / 1024) else String.format("%.0f MB", mb)
    }

    override fun getItemCount(): Int = list.size
}
