package eu.kanade.tachiyomi.event;

import eu.kanade.tachiyomi.data.database.models.Manga;

public class MangaEvent {

    public final Manga manga;

    public MangaEvent(Manga manga) {
        this.manga = manga;
    }
}
