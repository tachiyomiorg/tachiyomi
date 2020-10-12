package eu.kanade.tachiyomi.data.backup.full.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@ExperimentalSerializationApi
@Serializable
data class BackupHistory(
    @ProtoNumber(0) var url: String,
    @ProtoNumber(1) var lastRead: Long
)
