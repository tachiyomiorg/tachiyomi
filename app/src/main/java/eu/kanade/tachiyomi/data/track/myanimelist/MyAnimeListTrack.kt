package eu.kanade.tachiyomi.data.track.myanimelist

import eu.kanade.tachiyomi.data.database.models.Track
import java.util.Calendar

class MyAnimeListTrack(
    // entry_id: 0
    var entry_id: String,

    // manga_id: 2
    var manga_id: String,

    // add_manga[status]: 1
    var status: String,

    // add_manga[num_read_volumes]: 0
    var num_read_volumes: String,

    // last_completed_vol:
    var last_completed_vol: String,

    // add_manga[num_read_chapters]: 0
    var num_read_chapters: String,

    // add_manga[score]:
    var score: String,

    // add_manga[start_date][month]:
    var start_date_month: String, // [1-12]

    // add_manga[start_date][day]:
    var start_date_day: String,

    // add_manga[start_date][year]:
    var start_date_year: String,

    // add_manga[finish_date][month]:
    var finish_date_month: String, // [1-12]

    // add_manga[finish_date][day]:
    var finish_date_day: String,

    // add_manga[finish_date][year]:
    var finish_date_year: String,

    // add_manga[tags]:
    var tags: String,

    // add_manga[priority]: 0
    var priority: String,

    // add_manga[storage_type]:
    var storage_type: String,

    // add_manga[num_retail_volumes]: 0
    var num_retail_volumes: String,

    // add_manga[num_read_times]: 0
    var num_read_times: String,

    // add_manga[reread_value]:
    var reread_value: String,

    // add_manga[comments]:
    var comments: String,

    // add_manga[is_asked_to_discuss]: 0
    var is_asked_to_discuss: String,

    // add_manga[sns_post_type]: 0
    var sns_post_type: String,

    // submitIt: 0
    val submitIt: String = "0"
) {
    fun copyPersonalFrom(track: Track) {
        num_read_chapters = track.last_chapter_read.toString()
        val numScore = track.score.toInt()
        if (numScore in 1..9)
            score = numScore.toString()
        status = track.status.toString()
        if (track.started_reading_date != null) {
            start_date_month = (track.started_reading_date!!.get(Calendar.MONTH) + 1).toString()
            start_date_day = track.started_reading_date!!.get(Calendar.DAY_OF_MONTH).toString()
            start_date_year = track.started_reading_date!!.get(Calendar.YEAR).toString()
        }
        if (track.finished_reading_date != null) {
            finish_date_month = (track.finished_reading_date!!.get(Calendar.MONTH) + 1).toString()
            finish_date_day = track.finished_reading_date!!.get(Calendar.DAY_OF_MONTH).toString()
            finish_date_year = track.finished_reading_date!!.get(Calendar.YEAR).toString()
        }
    }
}
