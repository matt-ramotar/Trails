package org.mobilenativefoundation.trails.feat.savetrail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.mobilenativefoundation.trails.data.trail.TrailSync
import org.mobilenativefoundation.trails.data.trail.TrailSyncCause
import org.mobilenativefoundation.trails.data.trail.TrailSyncStatus
import org.mobilenativefoundation.trails.foundation.designsystem.component.StatusKind

class SyncStatusTest {
    @Test
    fun pendingCopyFollowsPlacementAndOffline() {
        val pending = TrailSync(TrailSyncStatus.PENDING, pendingCount = 1)
        assertEquals("Waiting to sync", syncStatus(pending, offline = false)!!.message)
        assertEquals("Waiting for a connection", syncStatus(pending, offline = true)!!.message)
        assertEquals("Saved on this device · Waiting to sync", syncStatus(pending, offline = false, detailed = true)!!.message)
        assertEquals("Saved here · Waiting for a connection", syncStatus(pending, offline = true, detailed = true)!!.message)
    }

    @Test
    fun retryableFailureIsFailedWithRetry() {
        val status = syncStatus(TrailSync(TrailSyncStatus.PENDING, canRetry = true), offline = false)!!
        assertEquals(StatusKind.FAILED, status.kind)
        assertTrue(status.canRetry)
        assertEquals("Sync needs attention · Local save is kept", status.message)
    }

    @Test
    fun parkedIncompatibleWorkAsksForAnUpdate() {
        val status = syncStatus(TrailSync(TrailSyncStatus.PARKED, cause = TrailSyncCause.INCOMPATIBLE), offline = false)!!
        assertEquals(StatusKind.ATTENTION, status.kind)
        assertEquals("App update needed to sync · Saved here", status.message)
    }

    /** Only the typed cause asks for an update; wording in a data-layer reason never does. */
    @Test
    fun parkedOtherCauseKeepsItsBoundedReasonEvenWhenItMentionsANewerApp() {
        val status = syncStatus(TrailSync(TrailSyncStatus.PARKED, reason = "Needs a newer app"), offline = false)!!
        assertEquals(StatusKind.ATTENTION, status.kind)
        assertEquals("Needs a newer app · Saved here", status.message)
        assertFalse(status.canRetry)
    }

    @Test
    fun parkedShortUnrecognisedReasonIsShownVerbatim() {
        val status = syncStatus(TrailSync(TrailSyncStatus.PARKED, reason = "Collection was removed"), offline = false)!!
        assertEquals(StatusKind.ATTENTION, status.kind)
        assertEquals("Collection was removed · Saved here", status.message)
        assertFalse(status.canRetry)
    }

    @Test
    fun parkedLongUnrecognisedReasonFallsBackToGenericCopy() {
        val status = syncStatus(TrailSync(TrailSyncStatus.PARKED, reason = "Collection was deleted on the server by another device"), offline = false)!!
        assertEquals(StatusKind.ATTENTION, status.kind)
        assertEquals("Sync is paused · Saved here", status.message)
        assertFalse(status.canRetry)
    }

    @Test
    fun parkedIncompatibleCauseOutranksALongReason() {
        val status = syncStatus(
            TrailSync(TrailSyncStatus.PARKED, reason = "Stored save bytes could not be decoded by this build", cause = TrailSyncCause.INCOMPATIBLE),
            offline = false,
        )!!
        assertEquals("App update needed to sync · Saved here", status.message)
        assertFalse(status.canRetry)
    }

    @Test
    fun syncedHasNoLine() = assertEquals(null, syncStatus(TrailSync(TrailSyncStatus.SYNCED), offline = false))
}
