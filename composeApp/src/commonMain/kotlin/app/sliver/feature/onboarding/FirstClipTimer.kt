package app.sliver.feature.onboarding

import app.sliver.core.log.logDebug
import kotlin.time.Clock

/** Debug measurement: first onboarding screen shown -> first clip saved (target under 30 s). */
object FirstClipTimer {
    private var startMs: Long? = null

    fun start() {
        if (startMs == null) startMs = Clock.System.now().toEpochMilliseconds()
    }

    fun clipSaved() {
        val start = startMs ?: return
        startMs = null
        logDebug("FirstClip", "first clip saved ${Clock.System.now().toEpochMilliseconds() - start} ms after onboarding start")
    }
}
