package dev.inmo.micro_utils.repos

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

fun createTableQuery(
    tableName: String,
    vararg columnsToTypes: Pair<String, ColumnType>
) = "create table $tableName (${columnsToTypes.joinToString(", ") { "${it.first} ${it.second}" }});"

fun SQLiteDatabase.createTable(
    tableName: String,
    vararg columnsToTypes: Pair<String, ColumnType>,
    onInit: (SQLiteDatabase.() -> Unit)? = null
): Boolean {
    val existing = rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'", null).use {
        it.count > 0
    }
    return if (existing) {
        false
        // TODO:: add upgrade opportunity
    } else {
        execSQL(createTableQuery(tableName, *columnsToTypes))
        onInit ?.invoke(this)
        true
    }
}

fun Cursor.getString(columnName: String) = getString(
    getColumnIndex(columnName)
)

fun Cursor.getLong(columnName: String) = getLong(
    getColumnIndex(columnName)
)

fun Cursor.getInt(columnName: String) = getInt(
    getColumnIndex(columnName)
)

fun Cursor.getDouble(columnName: String) = getDouble(
    getColumnIndex(columnName)
)

fun SQLiteDatabase.select(
    table: String,
    columns: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    groupBy: String? = null,
    having: String? = null,
    orderBy: String? = null,
    limit: String? = null
) = query(
    table, columns, selection, selectionArgs, groupBy, having, orderBy, limit
)

fun makePlaceholders(count: Int): String {
    return (0 until count).joinToString { "?" }
}

fun makeStringPlaceholders(count: Int): String {
    return (0 until count).joinToString { "\"?\"" }
}