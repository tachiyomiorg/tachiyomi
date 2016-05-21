package eu.kanade.tachiyomi.data.database.models;

/**
 * Object containing manga, chapter and history
 */
public class MangaChapterHistory {
    /**
     * Object containing manga and chapter
     */
    public MangaChapter mangaChapter;

    /**
     * Object containing history
     */
    public History history;

    /**
     * MangaChapterHistory constructor
     *
     * @param mangaChapter object containing manga and chapter
     * @param history      object containing history
     */
    public MangaChapterHistory(MangaChapter mangaChapter, History history) {
        this.mangaChapter = mangaChapter;
        this.history = history;
    }
}
