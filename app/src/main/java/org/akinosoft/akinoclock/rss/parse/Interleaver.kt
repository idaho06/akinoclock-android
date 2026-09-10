package org.akinosoft.akinoclock.rss.parse

import org.akinosoft.akinoclock.rss.model.Headline

/**
 * Round-robins headlines across feeds in configured order (feed A #1, feed B #1, feed C #1,
 * feed A #2, ...) so no single prolific feed dominates the carousel. Feeds that run out simply
 * stop contributing; the ones with headlines left keep going.
 */
object Interleaver {

    fun interleave(feeds: List<List<Headline>>): List<Headline> {
        val result = mutableListOf<Headline>()
        var index = 0
        while (true) {
            var addedAny = false
            for (feed in feeds) {
                if (index < feed.size) {
                    result += feed[index]
                    addedAny = true
                }
            }
            if (!addedAny) break
            index++
        }
        return result
    }
}
