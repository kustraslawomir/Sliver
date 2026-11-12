package app.sliver.core.time

import kotlin.time.Clock
import kotlin.time.Instant

/** System clock that tests can pin so "today" is stable in screenshots. */
object AppClock : Clock {
    var override: Clock? = null
    override fun now(): Instant = (override ?: Clock.System).now()
}
