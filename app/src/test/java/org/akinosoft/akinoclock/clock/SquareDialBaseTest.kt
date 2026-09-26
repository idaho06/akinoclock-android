package org.akinosoft.akinoclock.clock

import android.content.Context
import android.view.View.MeasureSpec
import androidx.test.core.app.ApplicationProvider
import org.akinosoft.akinoclock.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class SquareView(context: Context) : SquareDialBase(context, null)

@RunWith(RobolectricTestRunner::class)
class SquareDialBaseTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `measure passes through when both dimensions are EXACTLY`() {
        val view = SquareView(context())
        view.measure(
            MeasureSpec.makeMeasureSpec(500, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(300, MeasureSpec.EXACTLY),
        )
        assertEquals(500, view.measuredWidth)
        assertEquals(300, view.measuredHeight)
    }

    @Test
    fun `measure with two AT_MOST bounds chooses the largest square that fits`() {
        val view = SquareView(context())
        view.measure(
            MeasureSpec.makeMeasureSpec(440, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(600, MeasureSpec.AT_MOST),
        )
        assertEquals(440, view.measuredWidth)
        assertEquals(440, view.measuredHeight)
    }

    @Test
    fun `measure with both dimensions UNSPECIFIED uses the default square size`() {
        val view = SquareView(context())
        view.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )
        val expected = context().resources.getDimensionPixelSize(R.dimen.clock_default_size)
        assertEquals(expected, view.measuredWidth)
        assertEquals(expected, view.measuredHeight)
    }

    @Test
    fun `measure with an exact width and unspecified height sizes a square to the exact dimension`() {
        val view = SquareView(context())
        view.measure(
            MeasureSpec.makeMeasureSpec(350, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )
        assertEquals(350, view.measuredWidth)
        assertEquals(350, view.measuredHeight)
    }
}
