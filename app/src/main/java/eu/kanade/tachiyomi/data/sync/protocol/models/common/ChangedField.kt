package eu.kanade.tachiyomi.data.sync.protocol.models.common

data class ChangedField<T : Any?>(
        var date: Long, //Millis since epoch in UTC
        var value: T
)
