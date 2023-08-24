package dev.inmo.micro_utils.repos.cache.full

import dev.inmo.micro_utils.common.*
import dev.inmo.micro_utils.coroutines.SmartRWLocker
import dev.inmo.micro_utils.coroutines.launchSafelyWithoutExceptions
import dev.inmo.micro_utils.coroutines.withReadAcquire
import dev.inmo.micro_utils.coroutines.withWriteLock
import dev.inmo.micro_utils.pagination.Pagination
import dev.inmo.micro_utils.pagination.PaginationResult
import dev.inmo.micro_utils.repos.*
import dev.inmo.micro_utils.repos.cache.util.actualizeAll
import dev.inmo.micro_utils.repos.pagination.getAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

open class FullReadKeyValueCacheRepo<Key,Value>(
    protected open val parentRepo: ReadKeyValueRepo<Key, Value>,
    protected open val kvCache: KeyValueRepo<Key, Value>,
    protected val locker: SmartRWLocker = SmartRWLocker()
) : ReadKeyValueRepo<Key, Value>, FullCacheRepo {
    protected suspend inline fun <T> doOrTakeAndActualize(
        action: KeyValueRepo<Key, Value>.() -> Optional<T>,
        actionElse: ReadKeyValueRepo<Key, Value>.() -> T,
        actualize: KeyValueRepo<Key, Value>.(T) -> Unit
    ): T {
        locker.withReadAcquire {
            kvCache.action().onPresented { return it }
        }
        return parentRepo.actionElse().also {
            locker.withWriteLock { kvCache.actualize(it) }
        }
    }
    protected open suspend fun actualizeAll() {
        locker.withWriteLock {
            kvCache.clear()
            kvCache.set(parentRepo.getAll { keys(it) }.toMap())
        }
    }

    override suspend fun get(k: Key): Value? = doOrTakeAndActualize(
        { get(k) ?.optional ?: Optional.absent() },
        { get(k) },
        { set(k, it ?: return@doOrTakeAndActualize) }
    )

    override suspend fun values(pagination: Pagination, reversed: Boolean): PaginationResult<Value> = doOrTakeAndActualize(
        { values(pagination, reversed).takeIf { it.results.isNotEmpty() }.optionalOrAbsentIfNull },
        { values(pagination, reversed) },
        { if (it.results.isNotEmpty()) actualizeAll() }
    )

    override suspend fun count(): Long = doOrTakeAndActualize(
        { count().takeIf { it != 0L }.optionalOrAbsentIfNull },
        { count() },
        { if (it != 0L) actualizeAll() }
    )

    override suspend fun contains(key: Key): Boolean = doOrTakeAndActualize(
        { contains(key).takeIf { it }.optionalOrAbsentIfNull },
        { contains(key) },
        { if (it) parentRepo.get(key) ?.also { kvCache.set(key, it) } }
    )

    override suspend fun getAll(): Map<Key, Value> = doOrTakeAndActualize(
        { getAll().takeIf { it.isNotEmpty() }.optionalOrAbsentIfNull },
        { getAll() },
        { kvCache.actualizeAll(clear = true) { it } }
    )

    override suspend fun keys(pagination: Pagination, reversed: Boolean): PaginationResult<Key> = doOrTakeAndActualize(
        { keys(pagination, reversed).takeIf { it.results.isNotEmpty() }.optionalOrAbsentIfNull },
        { keys(pagination, reversed) },
        { if (it.results.isNotEmpty()) actualizeAll() }
    )

    override suspend fun keys(v: Value, pagination: Pagination, reversed: Boolean): PaginationResult<Key> = doOrTakeAndActualize(
        { keys(v, pagination, reversed).takeIf { it.results.isNotEmpty() }.optionalOrAbsentIfNull },
        { parentRepo.keys(v, pagination, reversed) },
        { if (it.results.isNotEmpty()) actualizeAll() }
    )

    override suspend fun invalidate() {
        actualizeAll()
    }
}

fun <Key, Value> ReadKeyValueRepo<Key, Value>.cached(
    kvCache: KeyValueRepo<Key, Value>,
    locker: SmartRWLocker = SmartRWLocker()
) = FullReadKeyValueCacheRepo(this, kvCache, locker)

open class FullWriteKeyValueCacheRepo<Key,Value>(
    parentRepo: WriteKeyValueRepo<Key, Value>,
    protected open val kvCache: KeyValueRepo<Key, Value>,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    protected val locker: SmartRWLocker = SmartRWLocker()
) : WriteKeyValueRepo<Key, Value> by parentRepo, FullCacheRepo {
    protected val onNewJob = parentRepo.onNewValue.onEach {
        locker.withWriteLock {
            kvCache.set(it.first, it.second)
        }
    }.launchIn(scope)
    protected val onRemoveJob = parentRepo.onValueRemoved.onEach {
        locker.withWriteLock {
            kvCache.unset(it)
        }
    }.launchIn(scope)

    override suspend fun invalidate() {
        locker.withWriteLock {
            kvCache.clear()
        }
    }
}

fun <Key, Value> WriteKeyValueRepo<Key, Value>.caching(
    kvCache: KeyValueRepo<Key, Value>,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) = FullWriteKeyValueCacheRepo(this, kvCache, scope)

open class FullKeyValueCacheRepo<Key,Value>(
    protected open val parentRepo: KeyValueRepo<Key, Value>,
    kvCache: KeyValueRepo<Key, Value>,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    skipStartInvalidate: Boolean = false,
    locker: SmartRWLocker = SmartRWLocker()
) : FullWriteKeyValueCacheRepo<Key,Value>(parentRepo, kvCache, scope),
    KeyValueRepo<Key,Value>,
    ReadKeyValueRepo<Key, Value> by FullReadKeyValueCacheRepo(
        parentRepo,
        kvCache,
        locker
) {
    init {
        if (!skipStartInvalidate) {
            scope.launchSafelyWithoutExceptions { invalidate() }
        }
    }

    override suspend fun unsetWithValues(toUnset: List<Value>) = parentRepo.unsetWithValues(toUnset)

    override suspend fun invalidate() {
        locker.withWriteLock {
            kvCache.actualizeAll(parentRepo)
        }
    }

    override suspend fun clear() {
        parentRepo.clear()
        kvCache.clear()
    }
}

fun <Key, Value> KeyValueRepo<Key, Value>.fullyCached(
    kvCache: KeyValueRepo<Key, Value> = MapKeyValueRepo(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    skipStartInvalidate: Boolean = false,
    locker: SmartRWLocker = SmartRWLocker()
) = FullKeyValueCacheRepo(this, kvCache, scope, skipStartInvalidate, locker)

@Deprecated("Renamed", ReplaceWith("this.fullyCached(kvCache, scope)", "dev.inmo.micro_utils.repos.cache.full.fullyCached"))
fun <Key, Value> KeyValueRepo<Key, Value>.cached(
    kvCache: KeyValueRepo<Key, Value>,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    skipStartInvalidate: Boolean = false,
    locker: SmartRWLocker = SmartRWLocker()
) = fullyCached(kvCache, scope, skipStartInvalidate, locker)
