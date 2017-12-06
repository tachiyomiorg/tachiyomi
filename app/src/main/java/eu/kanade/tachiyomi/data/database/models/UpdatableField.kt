package eu.kanade.tachiyomi.data.database.models

class UpdatableField(val parent: Updatable, val id: Int, val field: String, val defValue: Any)