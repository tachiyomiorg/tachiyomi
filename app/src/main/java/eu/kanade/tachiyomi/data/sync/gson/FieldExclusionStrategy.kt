package eu.kanade.tachiyomi.data.sync.gson

import com.google.gson.FieldAttributes
import com.google.gson.ExclusionStrategy

/**
 * Exclude fields by class and field name
 */
class FieldExclusionStrategy constructor(val entries: Map<Class<Any>, List<String>>) : ExclusionStrategy {
    override fun shouldSkipClass(arg0: Class<*>): Boolean {
        return false
    }

    override fun shouldSkipField(f: FieldAttributes): Boolean {
        val entry = entries[f.declaringClass] ?: return false
        return f.name in entry
    }
}