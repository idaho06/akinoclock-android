package org.akinosoft.akinoclock.util

import android.graphics.Bitmap
import android.graphics.Color

/** Squared ARGB distance between two colours; the alpha term is zero for two opaque colours. */
fun colorDistance(a: Int, b: Int): Int {
    val da = Color.alpha(a) - Color.alpha(b)
    val dr = Color.red(a) - Color.red(b)
    val dg = Color.green(a) - Color.green(b)
    val db = Color.blue(a) - Color.blue(b)
    return da * da + dr * dr + dg * dg + db * db
}

/** Pixel nearest to [target] in the square neighbourhood of [radius] px around (cx, cy). */
fun closestInNeighborhood(bitmap: Bitmap, cx: Int, cy: Int, radius: Int, target: Int): Int {
    var best = bitmap.getPixel(cx, cy)
    var bestDistance = colorDistance(best, target)
    for (x in (cx - radius)..(cx + radius)) {
        for (y in (cy - radius)..(cy + radius)) {
            if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height) continue
            val candidate = bitmap.getPixel(x, y)
            val distance = colorDistance(candidate, target)
            if (distance < bestDistance) {
                best = candidate
                bestDistance = distance
            }
        }
    }
    return best
}
