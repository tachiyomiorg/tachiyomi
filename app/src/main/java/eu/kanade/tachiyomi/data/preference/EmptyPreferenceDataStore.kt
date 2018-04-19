package eu.kanade.tachiyomi.data.preference

import android.support.v7.preference.PreferenceDataStore

class EmptyPreferenceDataStore : PreferenceDataStore() {

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return false
    }

    override fun putBoolean(key: String?, value: Boolean) {
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return 0
    }

    override fun putInt(key: String?, value: Int) {
    }

    override fun getLong(key: String?, defValue: Long): Long {
        return 0
    }

    override fun putLong(key: String?, value: Long) {
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        return 0f
    }

    override fun putFloat(key: String?, value: Float) {
    }

    override fun getString(key: String?, defValue: String?): String? {
        return null
    }

    override fun putString(key: String?, value: String?) {
    }

    override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? {
        return null
    }

    override fun putStringSet(key: String?, values: Set<String>?) {
    }
}