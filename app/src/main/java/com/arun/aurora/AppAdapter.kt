package com.arun.aurora

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItemAdapter(
    private val onClick: (Item) -> Unit,
    private val onLong: (Item) -> Unit,
    private val onFocus: (Item, View) -> Unit
) : RecyclerView.Adapter<ItemAdapter.VH>() {

    var accent: Int = 0xFF6EA8FF.toInt()
    var upFocusId: Int = View.NO_ID
    private var items: List<Item> = emptyList()

    fun submit(list: List<Item>) { items = list; notifyDataSetChanged() }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val label: TextView = v.findViewById(R.id.label)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_app, p, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = items[pos]
        val ctx = h.itemView.context
        val app = item.app
        if (app != null) {
            val pm = ctx.packageManager
            val banner = try { pm.getActivityBanner(app.component) } catch (_: Exception) { null }
            if (banner != null) {
                h.icon.scaleType = ImageView.ScaleType.CENTER_CROP
                h.icon.setImageDrawable(banner)
            } else {
                h.icon.scaleType = ImageView.ScaleType.FIT_CENTER
                h.icon.setImageDrawable(try { pm.getActivityIcon(app.component) } catch (_: Exception) { null })
            }
        } else {
            h.icon.scaleType = ImageView.ScaleType.FIT_XY
            h.icon.setImageDrawable(BadgeDrawable(item.badge ?: "?", item.c1, item.c2))
        }
        h.label.text = item.label
        if (upFocusId != View.NO_ID) h.itemView.nextFocusUpId = upFocusId
        h.itemView.background = Ui.tileBg(ctx, accent, false)
        h.itemView.setOnClickListener { onClick(item) }
        h.itemView.setOnLongClickListener { onLong(item); true }
        h.itemView.setOnFocusChangeListener { v, focused ->
            v.background = Ui.tileBg(ctx, accent, focused)
            val s = if (focused) 1.12f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(170).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
            v.translationZ = if (focused) 10f else 0f
            if (focused) onFocus(item, v)
        }
    }
}
