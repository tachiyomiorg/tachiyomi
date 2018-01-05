package eu.kanade.tachiyomi.data.sync.protocol.models.common

/**
 * Represents the change of a single property of an object
 */
data class ChangedField<T : Any?>(
        /**
         * Millis since epoch in UTC representing when this change occurred
         */
        var date: Long,
        
        /**
         * The new value of the changed field
         */
        var value: T
)
