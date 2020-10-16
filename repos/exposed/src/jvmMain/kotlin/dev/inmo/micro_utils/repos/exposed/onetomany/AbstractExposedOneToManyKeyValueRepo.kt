package dev.inmo.micro_utils.repos.exposed.onetomany

import dev.inmo.micro_utils.repos.OneToManyKeyValueRepo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

abstract class AbstractExposedOneToManyKeyValueRepo<Key, Value>(
    keyColumnAllocator: ColumnAllocator<Key>,
    valueColumnAllocator: ColumnAllocator<Value>,
    database: Database
) : OneToManyKeyValueRepo<Key, Value>, AbstractExposedReadOneToManyKeyValueRepo<Key, Value>(
    keyColumnAllocator,
    valueColumnAllocator,
    database
) {
    override suspend fun add(k: Key, v: Value) {
        transaction(database) {
            insert {
                it[keyColumn] = k
                it[valueColumn] = v
            }
        }
    }

    override suspend fun remove(k: Key, v: Value) {
        transaction(database) { deleteWhere { keyColumn.eq(k).and(valueColumn.eq(v)) } }
    }

    override suspend fun clear(k: Key) {
        transaction(database) { deleteWhere { keyColumn.eq(k) } }
    }
}

@Deprecated("Renamed", ReplaceWith("AbstractExposedOneToManyKeyValueRepo", "dev.inmo.micro_utils.repos.exposed.onetomany.AbstractExposedOneToManyKeyValueRepo"))
typealias AbstractOneToManyExposedKeyValueRepo<Key, Value> = AbstractExposedOneToManyKeyValueRepo<Key, Value>