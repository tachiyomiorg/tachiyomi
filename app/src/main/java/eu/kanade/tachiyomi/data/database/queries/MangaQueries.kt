package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.resolvers.*
import eu.kanade.tachiyomi.data.database.tables.CategoryTable
import eu.kanade.tachiyomi.data.database.tables.ChapterTable
import eu.kanade.tachiyomi.data.database.tables.MangaCategoryTable
import eu.kanade.tachiyomi.data.database.tables.MangaTable

interface MangaQueries : DbProvider {

    fun getMangas() = db.get()
            .listOfObjects(Manga::class.java)
            .withQuery(Query.builder()
                    .table(MangaTable.TABLE)
                    .build())
            .prepare()

    fun getLibraryMangas() = db.get()
            .listOfObjects(LibraryManga::class.java)
            .withQuery(RawQuery.builder()
                    .query(libraryQuery)
                    .observesTables(MangaTable.TABLE, ChapterTable.TABLE, MangaCategoryTable.TABLE, CategoryTable.TABLE)
                    .build())
            .withGetResolver(LibraryMangaGetResolver.INSTANCE)
            .prepare()

    fun getFavoriteMangas() = db.get()
            .listOfObjects(Manga::class.java)
            .withQuery(Query.builder()
                    .table(MangaTable.TABLE)
                    .where("${MangaTable.COL_FAVORITE} = ?")
                    .whereArgs(1)
                    .orderBy(MangaTable.COL_TITLE)
                    .build())
            .prepare()

    fun getManga(url: String, sourceId: Long) = db.get()
            .`object`(Manga::class.java)
            .withQuery(Query.builder()
                    .table(MangaTable.TABLE)
                    .where("${MangaTable.COL_URL} = ? AND ${MangaTable.COL_SOURCE} = ?")
                    .whereArgs(url, sourceId)
                    .build())
            .prepare()

    fun getManga(id: Long) = db.get()
            .`object`(Manga::class.java)
            .withQuery(Query.builder()
                    .table(MangaTable.TABLE)
                    .where("${MangaTable.COL_ID} = ?")
                    .whereArgs(id)
                    .build())
            .prepare()

    fun insertManga(manga: Manga) = db.put().`object`(manga).prepare()

    fun insertMangas(mangas: List<Manga>) = db.put().objects(mangas).prepare()

    fun updateFlags(manga: Manga) = db.put()
            .`object`(manga)
            .withPutResolver(MangaFlagsPutResolver())
            .prepare()

    fun updateLastUpdated(manga: Manga) = db.put()
            .`object`(manga)
            .withPutResolver(MangaLastUpdatedPutResolver())
            .prepare()

    fun updateMangaFavorite(manga: Manga) = db.put()
            .`object`(manga)
            .withPutResolver(MangaFavoritePutResolver())
            .prepare()

    fun updateMangaViewer(manga: Manga) = db.put()
            .`object`(manga)
            .withPutResolver(MangaViewerPutResolver())
            .prepare()

    fun updateMangaTitle(manga: Manga) = db.put()
            .`object`(manga)
            .withPutResolver(MangaTitlePutResolver())
            .prepare()

    fun deleteManga(manga: Manga) = db.delete().`object`(manga).prepare()

    fun deleteMangas(mangas: List<Manga>) = db.delete().objects(mangas).prepare()

    fun deleteMangasNotInLibrary() = db.delete()
            .byQuery(DeleteQuery.builder()
                    .table(MangaTable.TABLE)
                    .where("${MangaTable.COL_FAVORITE} = ?")
                    .whereArgs(0)
                    .build())
            .prepare()

    fun deleteMangas() = db.delete()
            .byQuery(DeleteQuery.builder()
                    .table(MangaTable.TABLE)
                    .build())
            .prepare()

    fun getLastReadManga() = db.get()
            .listOfObjects(Manga::class.java)
            .withQuery(RawQuery.builder()
                    .query(getLastReadMangaQuery())
                    .observesTables(MangaTable.TABLE)
                    .build())
            .prepare()

    fun getTotalChapterManga() = db.get().listOfObjects(Manga::class.java)
            .withQuery(RawQuery.builder().query(getTotalChapterMangaQuery()).observesTables(MangaTable.TABLE).build()).prepare();
}
