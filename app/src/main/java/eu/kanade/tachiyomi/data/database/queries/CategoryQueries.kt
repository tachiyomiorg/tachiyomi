package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.Query
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.tables.CategoryTable

interface CategoryQueries : DbProvider {

    fun getCategories() = db.get()
        .listOfObjects(Category::class.java)
        .withQuery(
            Query.builder()
                .table(CategoryTable.TABLE)
                .orderBy(CategoryTable.COL_ORDER)
                .build()
        )
        .prepare()

    fun getCategoriesForManga(manga: Manga) = db.get()
        .listOfObjects(Category::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getCategoriesForMangaQuery())
                .args(manga.id)
                .build()
        )
        .prepare()

    fun insertCategory(category: Category) = db.put().`object`(category).prepare()

    fun insertCategories(categories: List<Category>) = db.put().objects(categories).prepare()

    fun deleteCategory(category: Category) = db.delete().`object`(category).prepare()

    fun deleteCategories(categories: List<Category>) = db.delete().objects(categories).prepare()
}
