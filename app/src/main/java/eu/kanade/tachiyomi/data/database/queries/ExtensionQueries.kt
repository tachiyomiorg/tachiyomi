package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.impl.DefaultStorIOSQLite
import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.Extension
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.resolvers.ExtensionFavoritePutResolver
import eu.kanade.tachiyomi.data.database.tables.ExtensionTable


interface ExtensionQueries : DbProvider {

    fun insertExtension(extension: Extension) = db.put().`object`(extension).prepare()

    fun getFavoriteExtensions() = db.get()
            .listOfObjects(Extension::class.java)
            .withQuery(Query.builder()
                    .table(ExtensionTable.TABLE)
                    .build())
            .prepare()

    fun updateExtensionFavorite(extension: Extension) = db.put()
            .`object`(extension)
            .withPutResolver(ExtensionFavoritePutResolver())
            .prepare()

    fun removeExtensionFAvorite(extension: Extension)= db.delete().`object`(extension).prepare()

}
