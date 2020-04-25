package eu.kanade.tachiyomi.util.lang

import java.text.DateFormat
import java.util.Calendar
import java.util.Date

fun Date.toDateTimestampString(dateFormatter: DateFormat): String {
    val date = dateFormatter.format(this)
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(this)
    return "$date $time"
}

fun Date.toTimestampString(): String {
    return DateFormat.getTimeInstance(DateFormat.SHORT).format(this)
}

/**
 * Get date as time key
 *
 * @param date desired date
 * @return date as time key
 */
fun Long.toDateKey(): Date {
    val cal = Calendar.getInstance()
    cal.time = Date(this)
    cal[Calendar.HOUR_OF_DAY] = 0
    cal[Calendar.MINUTE] = 0
    cal[Calendar.SECOND] = 0
    cal[Calendar.MILLISECOND] = 0
    return cal.time
}

/**
 * Convert epoch long to Calendar instance
 *
 * @return Calendar instance at supplied epoch time. Null if epoch was 0.
 */
fun Long.toCalendar(): Calendar? {
    if (this == 0L) {
        return null
    }
    val cal = Calendar.getInstance()
    cal.timeInMillis = this
    return cal
}
