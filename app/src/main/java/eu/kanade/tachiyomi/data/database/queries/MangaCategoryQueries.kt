package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.Queries
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.inTransaction
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.tables.MangaCategoryTable

interface MangaCategoryQueries : DbProvider {

    fun insertMangaCategory(mangaCategory: MangaCategory) = db.put().`object`(mangaCategory).prepare()

    fun insertMangasCategories(mangasCategories: List<MangaCategory>) = db.put().objects(mangasCategories).prepare()

    fun deleteOldMangasCategories(mangas: List<Manga>) = db.delete()
        .byQuery(
            DeleteQuery.builder()
                .table(MangaCategoryTable.TABLE)
                .where("${MangaCategoryTable.COL_MANGA_ID} IN (${Queries.placeholders(mangas.size)})")
                .whereArgs(*mangas.map { it.id }.toTypedArray())
                .build()
        )
        .prepare()

    fun setMangaCategories(mangasCategories: List<MangaCategory>, mangas: List<Manga>) {
        db.inTransaction {
            deleteOldMangasCategories(mangas).executeAsBlocking()
            insertMangasCategories(mangasCategories).executeAsBlocking()
        }
    }
}
