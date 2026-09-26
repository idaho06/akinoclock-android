package org.akinosoft.akinoclock.clock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.akinosoft.akinoclock.util.closestInNeighborhood
import org.akinosoft.akinoclock.util.colorDistance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class TestableDialView(context: Context) : DialView(context) {
    var invalidateCount = 0
        private set

    override fun invalidate() {
        invalidateCount++
        super.invalidate()
    }
}

@RunWith(RobolectricTestRunner::class)
class DialViewTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun render(view: DialView, size: Int): Bitmap {
        view.layout(0, 0, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    /** Whether a pixel close to [target] exists within [radius] px of (cx, cy). */
    private fun hasColorNear(bitmap: Bitmap, cx: Int, cy: Int, radius: Int, target: Int): Boolean =
        colorDistance(closestInNeighborhood(bitmap, cx, cy, radius, target), target) < 30 * 30

    @Test
    fun `draws the bezel ring at the radius`() {
        val view = DialView(context())

        val bitmap = render(view, 200)

        assertTrue(hasColorNear(bitmap, 100, 1, 2, view.palette.bezelRing))
    }

    @Test
    fun `draws the 12 numeral`() {
        val view = DialView(context())

        val bitmap = render(view, 200)

        val p = ClockGeometry.numeralCenter(12, 100f)
        assertTrue(hasColorNear(bitmap, (100 + p.x).toInt(), (100 + p.y).toInt(), 6, view.palette.numeral))
    }

    @Test
    fun `leaves the background transparent so the window shows through`() {
        val bitmap = render(DialView(context()), 200)

        assertEquals(0, Color.alpha(bitmap.getPixel(2, 2)))
        assertEquals(0, Color.alpha(bitmap.getPixel(100, 100)))
    }

    @Test
    fun `a new palette redraws the dial in the new colours`() {
        val view = TestableDialView(context())
        render(view, 200)
        val before = view.invalidateCount

        view.palette = view.palette.copy(bezelRing = Color.RED)

        assertEquals(before + 1, view.invalidateCount)
        assertTrue(hasColorNear(render(view, 200), 100, 1, 2, Color.RED))
    }

    @Test
    fun `a size change redraws the dial at the new radius`() {
        val view = DialView(context())
        render(view, 200)

        val bitmap = render(view, 400)

        assertTrue(hasColorNear(bitmap, 200, 1, 2, view.palette.bezelRing))
        assertEquals(0, Color.alpha(bitmap.getPixel(100, 1)))
    }
}
