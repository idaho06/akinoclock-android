package org.akinosoft.akinoclock.rss.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.rss.model.Headline
import org.akinosoft.akinoclock.util.FixedIntervalScheduler
import org.akinosoft.akinoclock.util.PeriodicScheduler

/**
 * One headline at a time, crossfading to the next every 8 s while more than one is configured.
 * Tap fires [onHeadlineClick] with whichever headline is currently shown, only when it has a
 * link. Rotation is driven by an injectable [scheduler] so tests can fire ticks deterministically
 * instead of waiting on a real clock — mirrors `ClockView`'s scheduler injection.
 */
class HeadlineCarouselView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    var scheduler: PeriodicScheduler =
        FixedIntervalScheduler(Handler(Looper.getMainLooper()), ROTATION_INTERVAL_MILLIS)

    var onHeadlineClick: ((Headline) -> Unit)? = null

    constructor(context: Context, scheduler: PeriodicScheduler) : this(context) {
        this.scheduler = scheduler
    }

    private val inflater = LayoutInflater.from(context)
    private var frontChild = inflateItem()
    private var backChild = inflateItem().apply { alpha = 0f }
    private val emptyStateView = TextView(context).apply {
        text = context.getString(R.string.rss_no_headlines)
        visibility = GONE
    }

    private var headlines: List<Headline> = emptyList()
    private var pendingIndex = 0
    private var stale = false

    var visibleHeadline: Headline? = null
        private set

    val isShowingPlaceholder: Boolean
        get() = emptyStateView.visibility == VISIBLE

    init {
        addView(frontChild)
        addView(backChild)
        addView(emptyStateView)
        isClickable = true
        setOnClickListener { onBarClicked() }
    }

    fun setHeadlines(newHeadlines: List<Headline>) {
        val hadContent = headlines.isNotEmpty()
        headlines = newHeadlines
        pendingIndex = 0
        scheduler.stop()

        if (newHeadlines.isEmpty()) {
            visibleHeadline = null
            showEmptyState()
            return
        }

        frontChild.visibility = VISIBLE
        emptyStateView.visibility = GONE
        if (!hadContent) reveal(animate = false)
        if (newHeadlines.size > 1) scheduler.start { reveal(animate = true) }
    }

    fun setStale(isStale: Boolean) {
        stale = isStale
        updateGlyph(frontChild)
    }

    private fun onBarClicked() {
        val headline = visibleHeadline ?: return
        if (headline.link != null) onHeadlineClick?.invoke(headline)
    }

    private fun reveal(animate: Boolean) {
        if (headlines.isEmpty()) return
        val headline = headlines[pendingIndex]
        pendingIndex = (pendingIndex + 1) % headlines.size
        visibleHeadline = headline
        if (animate) {
            bind(backChild, headline)
            crossfadeToBack()
        } else {
            bind(frontChild, headline)
        }
    }

    private fun crossfadeToBack() {
        // Capture the outgoing/incoming views in locals before swapping the frontChild/backChild
        // fields below — the withEndAction lambda must hide the view that is actually fading
        // out, not whatever the mutable field happens to point to 400ms from now.
        val outgoing = frontChild
        val incoming = backChild
        incoming.alpha = 0f
        incoming.visibility = VISIBLE
        incoming.animate().alpha(1f).setDuration(CROSSFADE_DURATION_MS).start()
        outgoing.animate().alpha(0f).setDuration(CROSSFADE_DURATION_MS).withEndAction {
            outgoing.visibility = INVISIBLE
        }.start()
        frontChild = incoming
        backChild = outgoing
    }

    private fun bind(view: View, headline: Headline) {
        view.findViewById<TextView>(R.id.headlineFeedTitle).text = headline.feedTitle
        view.findViewById<TextView>(R.id.headlineTitle).text = headline.title
        updateGlyph(view)
    }

    private fun updateGlyph(view: View) {
        val glyph = view.findViewById<TextView>(R.id.headlineStatusGlyph)
        glyph.visibility = if (stale) VISIBLE else GONE
    }

    private fun showEmptyState() {
        frontChild.visibility = GONE
        backChild.visibility = GONE
        emptyStateView.visibility = VISIBLE
    }

    private fun inflateItem(): View = inflater.inflate(R.layout.item_headline, this, false)

    private companion object {
        const val ROTATION_INTERVAL_MILLIS = 8_000L
        const val CROSSFADE_DURATION_MS = 400L
    }
}
