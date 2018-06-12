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
        when (type) {
            "UPDATE" -> {
                // language=sql
                conditionBuilder += "old.${field.field} != new.${field.field}"
                triggerType = "AFTER UPDATE"
            }
            "INSERT" -> {
                //Ignore inserts if inserted values are the same as the default values
                conditionBuilder += "new.${field.field} != " + valueAsSQLiteLiteral(field.defValue)
                triggerType = "AFTER INSERT"
            }
            "DELETE" -> triggerType = "BEFORE DELETE"
            else -> throw IllegalArgumentException("Unknown query type: $type!")
        }

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
        const val CURRENT_TIME_SQL = "CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER)"
    
        /**
         * Convert a primitives and Strings into SQL literals
         */
        fun valueAsSQLiteLiteral(value: Any?)
                = when(value) {
            //Wrap strings in quotes
            is String, is Char -> "\"$value\""
        
            is Int, is Long, is Float, is Byte, is Short, is Double -> value.toString()
    
            //SQL does not have boolean literals
            false -> "0"
            true -> "1"

            null -> "NULL"
        
            else -> throw IllegalArgumentException("Unknown default value type: ${value::class.java.simpleName}!")
        }
    }
}