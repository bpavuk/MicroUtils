package dev.inmo.micro_utils.repos.exposed.onetomany

import dev.inmo.micro_utils.pagination.*
import dev.inmo.micro_utils.repos.ReadOneToManyKeyValueRepo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

typealias ColumnAllocator<T> = Table.() -> Column<T>

abstract class AbstractExposedReadOneToManyKeyValueRepo<Key, Value>(
    keyColumnAllocator: ColumnAllocator<Key>,
    valueColumnAllocator: ColumnAllocator<Value>,
    protected val database: Database
) : ReadOneToManyKeyValueRepo<Key, Value>, Table() {
    protected val keyColumn: Column<Key> = keyColumnAllocator()
    protected val valueColumn: Column<Value> = valueColumnAllocator()

    override suspend fun count(k: Key): Long = transaction(database) { select { keyColumn.eq(k) }.count() }

    override suspend fun count(): Long = transaction(database) { selectAll().count() }

    override suspend fun get(
        k: Key,
        pagination: Pagination,
        reversed: Boolean
    ): PaginationResult<Value> = transaction(database) {
        select { keyColumn.eq(k) }.paginate(pagination, keyColumn, reversed).map { it[valueColumn] }
    }.createPaginationResult(
        pagination,
        count(k)
    )

    override suspend fun keys(pagination: Pagination, reversed: Boolean): PaginationResult<Key> = transaction(database) {
        selectAll().paginate(pagination, keyColumn, reversed).map { it[keyColumn] }
    }.createPaginationResult(
        pagination,
        count()
    )

    override suspend fun contains(k: Key): Boolean = transaction(database) {
        select { keyColumn.eq(k) }.limit(1).any()
    }

    override suspend fun contains(k: Key, v: Value): Boolean = transaction(database) {
        select { keyColumn.eq(k).and(valueColumn.eq(v)) }.limit(1).any()
    }
}

@Deprecated("Renamed", ReplaceWith("AbstractExposedReadOneToManyKeyValueRepo", "dev.inmo.micro_utils.repos.exposed.onetomany.AbstractExposedReadOneToManyKeyValueRepo"))
typealias AbstractOneToManyExposedReadKeyValueRepo<Key, Value> = AbstractExposedReadOneToManyKeyValueRepo<Key, Value>