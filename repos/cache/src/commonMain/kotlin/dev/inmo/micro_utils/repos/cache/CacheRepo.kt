package dev.inmo.micro_utils.repos.cache

interface CacheRepo {
    suspend fun invalidate()
}
