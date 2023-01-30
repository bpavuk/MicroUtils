package dev.inmo.micro_utils.repos.cache.fallback.keyvalues

import dev.inmo.micro_utils.coroutines.plus
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.pagination.FirstPagePagination
import dev.inmo.micro_utils.pagination.utils.doForAllWithNextPaging
import dev.inmo.micro_utils.repos.WriteKeyValuesRepo
import dev.inmo.micro_utils.repos.cache.cache.FullKVCache
import dev.inmo.micro_utils.repos.cache.FallbackCacheRepo
import dev.inmo.micro_utils.repos.set
import dev.inmo.micro_utils.repos.unset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

open class AutoRecacheWriteKeyValuesRepo<Id, RegisteredObject>(
    protected val originalRepo: WriteKeyValuesRepo<Id, RegisteredObject>,
    protected val scope: CoroutineScope,
    protected val kvCache: FullKVCache<Id, List<RegisteredObject>> = FullKVCache()
) : WriteKeyValuesRepo<Id, RegisteredObject>, FallbackCacheRepo {
    override val onValueRemoved: Flow<Pair<Id, RegisteredObject>>
        get() = originalRepo.onValueRemoved

    override val onNewValue: Flow<Pair<Id, RegisteredObject>>
        get() = originalRepo.onNewValue
    override val onDataCleared: Flow<Id>
        get() = (originalRepo.onDataCleared + kvCache.onValueRemoved).distinctUntilChanged()

    private val onDataClearedListeningJob = originalRepo.onDataCleared.subscribeSafelyWithoutExceptions(scope) {
        kvCache.unset(it)
    }

    private val onRemovingUpdatesListeningJob = originalRepo.onValueRemoved.subscribeSafelyWithoutExceptions(scope) {
        kvCache.set(
            it.first,
            (kvCache.get(
                it.first
            ) ?: return@subscribeSafelyWithoutExceptions) - it.second
        )
    }

    private val onNewAndUpdatedObjectsListeningJob = originalRepo.onNewValue.subscribeSafelyWithoutExceptions(scope) {
        kvCache.set(
            it.first,
            (kvCache.get(
                it.first
            ) ?: return@subscribeSafelyWithoutExceptions) + it.second
        )
    }

    override suspend fun clearWithValue(v: RegisteredObject) {
        originalRepo.clearWithValue(v)
        doForAllWithNextPaging(FirstPagePagination(kvCache.count().takeIf { it < Int.MAX_VALUE } ?.toInt() ?: Int.MAX_VALUE)) {
            kvCache.keys(it).also {
                it.results.forEach { id ->
                    kvCache.get(id) ?.takeIf { it.contains(v) } ?.let {
                        kvCache.unset(id)
                    }
                }
            }
        }
    }

    override suspend fun clear(k: Id) {
        originalRepo.clear(k)
        kvCache.unset(k)
    }

    override suspend fun remove(toRemove: Map<Id, List<RegisteredObject>>) {
        originalRepo.remove(toRemove)
        toRemove.forEach { (k, v) ->
            kvCache.set(k, (kvCache.get(k) ?: return@forEach) - v)
        }
    }

    override suspend fun add(toAdd: Map<Id, List<RegisteredObject>>) {
        originalRepo.add(toAdd)
        toAdd.forEach { (k, v) ->
            kvCache.set(k, (kvCache.get(k) ?: return@forEach) + v)
        }
    }
}
