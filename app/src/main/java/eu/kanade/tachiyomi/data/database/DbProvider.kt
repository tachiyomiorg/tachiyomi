package eu.kanade.tachiyomi.data.database

import com.pushtorefresh.storio.sqlite.impl.DefaultStorIOSQLite

interface DbProvider {

    val db: DefaultStorIOSQLite
}
