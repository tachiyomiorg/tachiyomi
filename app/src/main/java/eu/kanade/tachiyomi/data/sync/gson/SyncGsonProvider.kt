package xyz.nulldev.ts.sync.gson

import com.google.gson.GsonBuilder
import eu.kanade.tachiyomi.data.sync.protocol.models.common.SyncEntity

object SyncGsonProvider {
    val gson = GsonBuilder()
            .registerTypeAdapter(SyncEntity::class.java, SyncEntityAdapter())
            .setPrettyPrinting() //TODO Remove
            .create() //TODO Add field exclusions
}