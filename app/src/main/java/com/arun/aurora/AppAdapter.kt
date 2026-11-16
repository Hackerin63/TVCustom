package com.arun.aurora

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val onClick: (AppEntry) -> Unit,
    private val onLong: (AppEntry) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {

    private var items: List<AppEntry> = emptyList()

    fun submit(list: List<AppEntry>) { items = list; notifyDataSetChanged() }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val label: TextView = v.findViewById(R.id.label)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_app, p, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val e = items[pos]
        val pm = h.itemView.context.packageManager
        val banner = try { pm.getActivityBanner(e.component) } catch (_: Exception) { null }
        if (banner != null) {
            h.icon.scaleType = ImageView.ScaleType.CENTER_CROP
            h.icon.setImageDrawable(banner)
        } else {
            h.icon.scaleType = ImageView.ScaleType.FIT_CENTER
            h.icon.setImageDrawable(try { pm.getActivityIcon(e.component) } catch (_: Exception) { null })
        }
        h.label.text = e.label
        h.itemView.setOnClickListener { onClick(e) }
        h.itemView.setOnLongClickListener { onLong(e); true }
        h.itemView.setOnFocusChangeListener { v, focused ->
            val s = if (focused) 1.1f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(120).start()
            v.translationZ = if (focused) 8f else 0f
        }
    }
}
