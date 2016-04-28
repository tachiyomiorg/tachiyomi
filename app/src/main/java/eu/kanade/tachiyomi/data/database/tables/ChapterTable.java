package eu.kanade.tachiyomi.data.database.tables;

import android.support.annotation.NonNull;

public final class ChapterTable {

    @NonNull
    public static final String TABLE = "chapters";

	@NonNull
	public static final String COLUMN_ID = "_id";

	@NonNull
	public static final String COLUMN_MANGA_ID = "manga_id";

	@NonNull
	public static final String COLUMN_URL = "url";

	@NonNull
	public static final String COLUMN_NAME = "name";

	@NonNull
	public static final String COLUMN_READ = "read";

	@NonNull
	public static final String COLUMN_DATE_FETCH = "date_fetch";

	@NonNull
	public static final String COLUMN_DATE_UPLOAD = "date_upload";

	@NonNull
	public static final String COLUMN_LAST_PAGE_READ = "last_page_read";

	@NonNull
	public static final String COLUMN_CHAPTER_NUMBER = "chapter_number";

	private ChapterTable() throws InstantiationException {
		throw new InstantiationException("This class is not for instantiation");
	}

	@NonNull
	public static String getCreateTableQuery() {
		return "CREATE TABLE " + TABLE + "("
				+ COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY, "
				+ COLUMN_MANGA_ID + " INTEGER NOT NULL, "
				+ COLUMN_URL + " TEXT NOT NULL, "
				+ COLUMN_NAME + " TEXT NOT NULL, "
				+ COLUMN_READ + " BOOLEAN NOT NULL, "
				+ COLUMN_LAST_PAGE_READ + " INT NOT NULL, "
				+ COLUMN_CHAPTER_NUMBER + " FLOAT NOT NULL, "
				+ COLUMN_DATE_FETCH + " LONG NOT NULL, "
				+ COLUMN_DATE_UPLOAD + " LONG NOT NULL, "
				+ "FOREIGN KEY(" + COLUMN_MANGA_ID + ") REFERENCES " + MangaTable.TABLE + "(" + MangaTable.COLUMN_ID + ") "
				+ "ON DELETE CASCADE"
				+ ");";
	}

	public static String getCreateMangaIdIndexQuery() {
		return "CREATE INDEX " + TABLE + "_" + COLUMN_MANGA_ID + "_index ON " + TABLE + "(" + COLUMN_MANGA_ID + ");";

	}
	
}
