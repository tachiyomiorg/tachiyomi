package eu.kanade.tachiyomi.data.database.tables;

import android.support.annotation.NonNull;

public final class MangaSyncTable {

    public static final String TABLE = "manga_sync";

    public static final String COLUMN_ID = "_id";

    public static final String COLUMN_MANGA_ID = "manga_id";

    public static final String COLUMN_SYNC_ID = "sync_id";

    public static final String COLUMN_REMOTE_ID = "remote_id";

    public static final String COLUMN_TITLE = "title";

    public static final String COLUMN_LAST_CHAPTER_READ = "last_chapter_read";

    public static final String COLUMN_STATUS = "status";

    public static final String COLUMN_SCORE = "score";

    public static final String COLUMN_TOTAL_CHAPTERS = "total_chapters";

    private MangaSyncTable() throws InstantiationException {
        throw new InstantiationException("This class is not for instantiation");
    }

    @NonNull
    public static String getCreateTableQuery() {
        return "CREATE TABLE " + TABLE + "("
                + COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY, "
                + COLUMN_MANGA_ID + " INTEGER NOT NULL, "
                + COLUMN_SYNC_ID + " INTEGER NOT NULL, "
                + COLUMN_REMOTE_ID + " INTEGER NOT NULL, "
                + COLUMN_TITLE + " TEXT NOT NULL, "
                + COLUMN_LAST_CHAPTER_READ + " INTEGER NOT NULL, "
                + COLUMN_TOTAL_CHAPTERS + " INTEGER NOT NULL, "
                + COLUMN_STATUS + " INTEGER NOT NULL, "
                + COLUMN_SCORE + " FLOAT NOT NULL, "
                + "UNIQUE (" + COLUMN_MANGA_ID + ", " + COLUMN_SYNC_ID + ") ON CONFLICT REPLACE, "
                + "FOREIGN KEY(" + COLUMN_MANGA_ID + ") REFERENCES " + MangaTable.TABLE + "(" + MangaTable.COLUMN_ID + ") "
                + "ON DELETE CASCADE"
                + ");";
    }

}
