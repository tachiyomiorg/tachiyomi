package eu.kanade.tachiyomi.data.database.models;

import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteColumn;
import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteType;

import java.io.Serializable;

import eu.kanade.tachiyomi.data.database.tables.HistoryTable;

/**
 * Object containing the history statistics of a chapter
 */
@StorIOSQLiteType(table = HistoryTable.TABLE)
public class History implements Serializable {

    /**
     * Id of history object.
     */
    @StorIOSQLiteColumn(name = HistoryTable.COL_ID, key = true)
    public Long id;

    /**
     * Chapter id of history object.
     */
    @StorIOSQLiteColumn(name = HistoryTable.COL_CHAPTER_ID)
    public long chapter_id;

    /**
     * Last time chapter was read in time long format
     */
    @StorIOSQLiteColumn(name = HistoryTable.COL_LAST_READ)
    public long last_read;

    /**
     * Total time chapter was read - todo not yet implemented
     */
    @StorIOSQLiteColumn(name = HistoryTable.COL_TIME_READ)
    public long time_read;

    /**
     * Empty history constructor
     */
    public History() {
    }

    /**
     * History constructor
     *
     * @param chapter chapter object
     * @return history object
     */
    public static History create(Chapter chapter) {
        History history = new History();
        history.chapter_id = chapter.id;
        return history;
    }
}

