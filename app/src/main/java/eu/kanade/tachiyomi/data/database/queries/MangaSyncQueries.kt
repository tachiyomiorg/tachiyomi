package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.data.database.tables.MangaSyncTable
import eu.kanade.tachiyomi.data.mangasync.MangaSyncService

interface MangaSyncQueries : DbProvider {

    fun getMangaSync(manga: Manga, sync: MangaSyncService) = db.get()
            .`object`(MangaSync::class.java)
            .withQuery(Query.builder()
                    .table(MangaSyncTable.TABLE)
                    .where("${MangaSyncTable.COL_MANGA_ID} = ? AND " +
                            "${MangaSyncTable.COL_SYNC_ID} = ?")
                    .whereArgs(manga.id, sync.id)
                    .build())
            .prepare()

    fun getMangasSync(manga: Manga) = db.get()
            .listOfObjects(MangaSync::class.java)
            .withQuery(Query.builder()
                    .table(MangaSyncTable.TABLE)
                    .where("${MangaSyncTable.COL_MANGA_ID} = ?")
                    .whereArgs(manga.id)
                    .build())
            .prepare()

    fun insertMangaSync(manga: MangaSync) = db.put().`object`(manga).prepare()

    fun insertMangasSync(mangas: List<MangaSync>) = db.put().objects(mangas).prepare()

    fun deleteMangaSync(manga: MangaSync) = db.delete().`object`(manga).prepare()

    fun deleteMangaSyncForManga(manga: Manga) = db.delete()
            .byQuery(DeleteQuery.builder()
                    .table(MangaSyncTable.TABLE)
                    .where("${MangaSyncTable.COL_MANGA_ID} = ?")
                    .whereArgs(manga.id)
                    .build())
            .prepare()

    fun deleteMangaSyncForManga(manga: Manga, sync: MangaSyncService) = db.delete()
            .byQuery(DeleteQuery.builder()
                    .table(MangaSyncTable.TABLE)
                    .where("${MangaSyncTable.COL_MANGA_ID} = ? AND ${MangaSyncTable.COL_SYNC_ID} = ?")
                    .whereArgs(manga.id, sync.id)
                    .build())
            .prepare()

}