package eu.kanade.tachiyomi.data.database

import eu.kanade.tachiyomi.data.database.models.UpdatableField
import eu.kanade.tachiyomi.data.database.tables.SyncUpdatesTable

class TriggerGenerator {
    fun genTriggers(field: UpdatableField)
        = listOf(
                genTrigger("INSERT", field),
                genTrigger("UPDATE", field),
                genDeleteTrigger(field)
        )

    private fun genDeleteTrigger(field: UpdatableField): String {
        // language=sql
        return """
            CREATE TRIGGER IF NOT EXISTS ${triggerName("DELETE", field)}
              BEFORE DELETE ON ${field.parent.tableName}
              BEGIN
                /* Delete entry update */
                DELETE FROM ${SyncUpdatesTable.TABLE}
                  WHERE ${SyncUpdatesTable.COL_UPDATED_ROW} = old.${field.parent.idColumn}
                    AND ${SyncUpdatesTable.COL_FIELD} = ${field.id};
              END;
        """
    }

    private fun genTrigger(type: String, field: UpdatableField): String {
        val type = type.toUpperCase()
        val triggerType: String

        val conditionBuilder = mutableListOf<String>()
        if(type == "UPDATE") {
            // language=sql
            conditionBuilder += "old.${field.field} != new.${field.field}"
            triggerType = "AFTER UPDATE"
        } else if(type == "INSERT") {
            //Ignore inserts if inserted values are the same as the default values
            conditionBuilder += "new.${field.field} != " + when(field.defValue) {
                //Wrap strings in quotes
                is String, is Char -> "\"${field.defValue}\""

                is Int, is Long, is Float, is Byte, is Short, is Double -> field.defValue

                //SQL does not have boolean literals
                false -> "0"
                true -> "1"

                else -> throw IllegalArgumentException("Unknown default value type: ${field.defValue::class.java.simpleName}!")
            }
            triggerType = "AFTER INSERT"
        } else if(type == "DELETE") {
            triggerType = "BEFORE DELETE"
        } else throw IllegalArgumentException("Unknown query type: $type!")

        val condition = if(conditionBuilder.isNotEmpty())
            conditionBuilder.joinToString(prefix = "WHEN ", separator = " AND ")
        else ""

        val entry = if(type == "INSERT") "new" else "old"

        // language=sql
        return """
            CREATE TRIGGER IF NOT EXISTS ${triggerName(type, field)}
              $triggerType ON ${field.parent.tableName}
              $condition
              BEGIN
                /* Delete old entry update */
                DELETE FROM ${SyncUpdatesTable.TABLE}
                  WHERE ${SyncUpdatesTable.COL_UPDATED_ROW} = $entry.${field.parent.idColumn}
                    AND ${SyncUpdatesTable.COL_FIELD} = ${field.id};

                /* Insert new entry update */
                INSERT INTO ${SyncUpdatesTable.TABLE} (
                    ${SyncUpdatesTable.COL_UPDATED_ROW},
                    ${SyncUpdatesTable.COL_DATETIME},
                    ${SyncUpdatesTable.COL_FIELD}
                ) VALUES (
                    $entry.${field.parent.idColumn},
                    $CURRENT_TIME_SQL,
                    ${field.id}
                );
              END;
        """
    }

    private fun triggerName(type: String, field: UpdatableField)
        = "sync_" + type.toLowerCase() + "_" + field.id + "_trigger"

    companion object {
        // Expression to get milliseconds since epoch in SQLITE
        //language=sql
        private const val CURRENT_TIME_SQL = "CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER)"
    }
}