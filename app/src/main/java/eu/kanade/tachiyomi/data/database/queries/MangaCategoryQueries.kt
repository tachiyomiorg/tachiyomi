package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.Queries
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.R.string.categories
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.inTransaction
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.tables.MangaCategoryTable

interface MangaCategoryQueries : DbProvider {

    fun insertMangaCategory(mangaCategory: MangaCategory) = db.put().`object`(mangaCategory).prepare()

    fun insertMangasCategories(mangasCategories: List<MangaCategory>) = db.put().objects(mangasCategories).prepare()

    fun deleteOldMangasCategories(mangas: List<Manga>) = db.delete()
            .byQuery(DeleteQuery.builder()
                    .table(MangaCategoryTable.TABLE)
                    .where("${MangaCategoryTable.COL_MANGA_ID} IN (${Queries.placeholders(mangas.size)})")
                    .whereArgs(*mangas.map { it.id }.toTypedArray())
                    .build())
            .prepare()
    
    fun deleteMangaCategory(mangasCategory: MangaCategory) = db.delete()
            .byQuery(DeleteQuery.builder()
                    .table(MangaCategoryTable.TABLE)
                    .where("${MangaCategoryTable.COL_MANGA_ID} = ? AND ${MangaCategoryTable.COL_CATEGORY_ID} = ?")
                    .whereArgs(mangasCategory.manga_id, mangasCategory.category_id)
                    .build())
            .prepare()

    fun setMangaCategories(mangasCategories: List<MangaCategory>, mangas: List<Manga>) {
        db.inTransaction {
            deleteOldMangasCategories(mangas).executeAsBlocking()
            insertMangasCategories(mangasCategories).executeAsBlocking()
        }
    }
    
    fun takeMangaCategoriesSnapshot() = db.executeSQL()
            .withQuery(RawQuery.builder()
                    .query(cloneMangaCategoriesQuery)
                    .build())
            .prepare()
    
    fun deleteMangaCategoriesSnapshot() = db.executeSQL()
            .withQuery(RawQuery.builder()
                    .query(deleteClonedMangaCategoriesQuery)
                    .build())
            .prepare()
    
    fun getAddedMangaCategories() = db.get()
            .listOfObjects(MangaCategory::class.java)
            .withQuery(RawQuery.builder()
                    .query(getAddedMangaCategoriesQuery)
                    .build())
            .prepare()
    
    fun getDeletedMangaCategories() = db.get()
            .listOfObjects(MangaCategory::class.java)
            .withQuery(RawQuery.builder()
                    .query(getDeletedMangaCategoriesQuery)
                    .build())
            .prepare()
}