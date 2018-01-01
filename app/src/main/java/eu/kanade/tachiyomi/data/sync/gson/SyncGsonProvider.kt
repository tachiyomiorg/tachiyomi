package eu.kanade.tachiyomi.data.sync.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import eu.kanade.tachiyomi.data.sync.protocol.models.common.SyncEntity
import xyz.nulldev.ts.sync.gson.SyncEntityAdapter

object SyncGsonProvider {
    val gson = GsonBuilder()
            .registerTypeAdapter(SyncEntity::class.java, SyncEntityAdapter())
            .setPrettyPrinting() //TODO Remove
            .create() //TODO Add field exclusions
    
    val snapshotGson = GsonBuilder().create()
}