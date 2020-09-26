package dev.inmo.micro_utils.common

fun <T> Iterable<T>.syncWith(
    other: Iterable<T>,
    removed: (List<T>) -> Unit = {},
    added: (List<T>) -> Unit = {}
) {
    removed(filter { it !in other })
    added(other.filter { it !in this })
}
