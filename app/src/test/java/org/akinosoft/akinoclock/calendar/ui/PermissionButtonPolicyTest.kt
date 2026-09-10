package org.akinosoft.akinoclock.calendar.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionButtonPolicyTest {

    @Test
    fun `never asked yet requests the permission regardless of rationale`() {
        assertEquals(
            PermissionAction.REQUEST,
            PermissionButtonPolicy.decide(alreadyAsked = false, canShowRationale = false),
        )
    }

    @Test
    fun `asked once and the system still allows a rationale requests again`() {
        assertEquals(
            PermissionAction.REQUEST,
            PermissionButtonPolicy.decide(alreadyAsked = true, canShowRationale = true),
        )
    }

    @Test
    fun `asked and the system refuses a rationale opens settings`() {
        assertEquals(
            PermissionAction.OPEN_SETTINGS,
            PermissionButtonPolicy.decide(alreadyAsked = true, canShowRationale = false),
        )
    }
}
