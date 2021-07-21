package eu.kanade.tachiyomi.data.track.kitsu

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object KitsuDateHelper {

    private const val pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    private val formatter = SimpleDateFormat(pattern, Locale.ENGLISH)

    fun convert(dateValue: Long): String? {
        if (dateValue == 0L) return null

        return formatter.format(Date(dateValue))
    }

    fun parse(dateString: String?): Long {
        if (dateString == null) return 0L

        val dateValue = formatter.parse(dateString)

        return dateValue?.time ?: return 0
    }
}
