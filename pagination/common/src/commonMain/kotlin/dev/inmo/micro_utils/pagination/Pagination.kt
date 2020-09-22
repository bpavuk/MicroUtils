package dev.inmo.micro_utils.pagination

import kotlin.math.ceil

/**
 * Base interface of pagination
 *
 * If you want to request something, you should use [SimplePagination]. If you need to return some result including
 * pagination - [PaginationResult]
 */
interface Pagination {
    /**
     * Started with 0.
     * Number of page inside of pagination. Offset can be calculated as [page] * [size]
     */
    val page: Int
    /**
     * Can be 0, but can't be < 0
     * Size of current page. Offset can be calculated as [page] * [size]
     */
    val size: Int
}

/**
 * First number in index of objects. It can be used as offset for databases or other data sources
 */
val Pagination.firstIndex: Int
    get() = page * size

/**
 * Last number in index of objects. In fact, one [Pagination] object represent data in next range:
 *
 * [[firstIndex], [lastIndex]]; That means, that for [Pagination] with [Pagination.size] == 10 and [Pagination.page] == 1
 * you will retrieve [Pagination.firstIndex] == 10 and [Pagination.lastIndex] == 19.
 */
val Pagination.lastIndex: Int
    get() = firstIndex + size - 1

/**
 * Calculates pages count for given [datasetSize]
 */
fun calculatePagesNumber(datasetSize: Long, pageSize: Int): Int {
    return ceil(datasetSize.toDouble() / pageSize).toInt()
}
/**
 * Calculates pages count for given [datasetSize]
 */
fun calculatePagesNumber(datasetSize: Int, pageSize: Int): Int =
    calculatePagesNumber(
        datasetSize.toLong(),
        pageSize
    )