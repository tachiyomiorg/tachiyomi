package eu.kanade.tachiyomi.data.backup.full.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializer

@ExperimentalSerializationApi
@Serializer(forClass = Backup::class)
object BackupSerializer
