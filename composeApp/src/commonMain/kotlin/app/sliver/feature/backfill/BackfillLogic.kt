package app.sliver.feature.backfill

import app.sliver.feature.archive.YearMonth
import app.sliver.feature.archive.toYearMonth
import app.sliver.feature.gallery.GalleryItem
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

enum class BackfillRange(val label: String) {
    SINCE_START("Since I started"),
    THIS_YEAR("This year"),
    EVERYTHING("Everything"),
}

/** First date (inclusive) the scan covers; null means no lower bound. */
fun rangeStart(range: BackfillRange, firstDay: LocalDate, today: LocalDate): LocalDate? = when (range) {
    BackfillRange.SINCE_START -> firstDay
    BackfillRange.THIS_YEAR -> LocalDate(today.year, 1, 1)
    BackfillRange.EVERYTHING -> null
}

@Serializable
data class BackfillItem(val date: String, val uri: String, val video: Boolean)

/**
 * One candidate per empty past day: the longest video, else (with [usePhotos]) the first photo.
 * Days that already have a second, today and the future are never candidates.
 */
fun findCandidates(
    items: List<GalleryItem>,
    filled: Set<LocalDate>,
    today: LocalDate,
    from: LocalDate?,
    usePhotos: Boolean,
): List<BackfillItem> = items
    .filter { it.date < today && it.date !in filled && (from == null || it.date >= from) }
    .groupBy { it.date }
    .mapNotNull { (date, group) ->
        val pick = group.filter { it.isVideo }.maxByOrNull { it.durationMs }
            ?: group.firstOrNull { !it.isVideo }.takeIf { usePhotos }
        pick?.let { BackfillItem(date.toString(), it.uri, it.isVideo) }
    }
    .sortedBy { it.date }

fun BackfillItem.month(): YearMonth = LocalDate.parse(date).toYearMonth()

/** Candidates of months the user has not switched off. */
fun selectedCandidates(candidates: List<BackfillItem>, off: Set<YearMonth>): List<BackfillItem> =
    candidates.filter { it.month() !in off }

data class YearRow(val year: Int, val counts: List<Int>)

/** Mosaic rows, newest year first; counts[0] is January. */
fun yearRows(candidates: List<BackfillItem>): List<YearRow> =
    candidates.groupBy { it.month().year }.entries.sortedByDescending { it.key }.map { (year, list) ->
        val perMonth = list.groupingBy { it.month().month }.eachCount()
        YearRow(year, List(12) { perMonth[it + 1] ?: 0 })
    }

@Serializable
data class BackfillBatch(val items: List<BackfillItem>, val processed: Set<String> = emptySet()) {
    val total get() = items.size
    val done get() = processed.size
    val finished get() = processed.size >= items.size
    val progress get() = if (items.isEmpty()) 1f else done.toFloat() / total
}

fun fillLabel(count: Int) = "Fill $count ${if (count == 1) "day" else "days"}"
