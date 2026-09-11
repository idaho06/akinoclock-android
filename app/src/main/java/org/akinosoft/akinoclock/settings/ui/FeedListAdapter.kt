package org.akinosoft.akinoclock.settings.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.rss.model.FeedConfig

class FeedListAdapter(
    context: Context,
    private val onDeleteClick: (FeedConfig) -> Unit,
) : ArrayAdapter<FeedConfig>(context, R.layout.item_feed) {

    private class ViewHolder(view: View) {
        val title: TextView = view.findViewById(R.id.feedTitle)
        val url: TextView = view.findViewById(R.id.feedUrl)
        val delete: View = view.findViewById(R.id.deleteFeedButton)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_feed, parent, false)
        val holder = view.tag as? ViewHolder ?: ViewHolder(view).also { view.tag = it }
        val feed = getItem(position) ?: return view

        if (feed.title != null) {
            holder.title.text = feed.title
            holder.url.text = feed.url
            holder.url.visibility = View.VISIBLE
        } else {
            holder.title.text = feed.url
            holder.url.visibility = View.GONE
        }
        holder.delete.setOnClickListener { onDeleteClick(feed) }

        return view
    }
}
