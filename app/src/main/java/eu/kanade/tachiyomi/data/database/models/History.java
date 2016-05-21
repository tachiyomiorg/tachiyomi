package eu.kanade.tachiyomi.data.database.models;

import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteColumn;
import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteType;

import eu.kanade.tachiyomi.data.database.tables.HistoryTable;

@StorIOSQLiteType(table = HistoryTable.TABLE)
public class History {

    @StorIOSQLiteColumn(name = HistoryTable.COL_ID, key = true)
    public Long id;

    @StorIOSQLiteColumn(name = HistoryTable.COL_MANGA_ID)
    public long manga_id;

    @StorIOSQLiteColumn(name = HistoryTable.COL_LAST_READ)
    public long last_read;

    public History() {
    }

    public static History create(Manga manga) {
        History history = new History();
        history.manga_id = manga.id;
        return history;
    }

}
