package dev.inmo.micro_utils.repos.ktor.server.key_value

import dev.inmo.micro_utils.ktor.server.*
import dev.inmo.micro_utils.pagination.PaginationResult
import dev.inmo.micro_utils.pagination.extractPagination
import dev.inmo.micro_utils.repos.ReadStandardKeyValueRepo
import dev.inmo.micro_utils.repos.ktor.common.key_value.*
import dev.inmo.micro_utils.repos.ktor.common.valueParameterName
import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.get
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

fun <K, V> Route.configureReadStandartKeyValueRepoRoutes (
    originalRepo: ReadStandardKeyValueRepo<K, V>,
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
    valueNullableSerializer: KSerializer<V?>,
) {
    get(getRoute) {
        val key = call.decodeUrlQueryValueOrSendError(
            keyParameterName,
            keySerializer
        ) ?: return@get

        call.unianswer(
            valueNullableSerializer,
            originalRepo.get(key)
        )
    }

    get(valuesRoute) {
        val parination = call.request.queryParameters.extractPagination;
        val reversed = call.decodeUrlQueryValueOrSendError(
            reversedParameterName,
            Boolean.serializer()
        ) ?: return@get

        call.unianswer(
            PaginationResult.serializer(valueSerializer),
            originalRepo.values(parination, reversed)
        )
    }

    get(keysRoute) {
        val parination = call.request.queryParameters.extractPagination;
        val reversed = call.decodeUrlQueryValueOrSendError(
            reversedParameterName,
            Boolean.serializer()
        ) ?: return@get
        val value = call.decodeUrlQueryValue(valueParameterName, valueSerializer)

        call.unianswer(
            PaginationResult.serializer(keySerializer),
            value ?.let { originalRepo.keys(value, parination, reversed) } ?: originalRepo.keys(parination, reversed)
        )
    }

    get(containsRoute) {
        val key = call.decodeUrlQueryValueOrSendError(
            keyParameterName,
            keySerializer
        ) ?: return@get

        call.unianswer(
            Boolean.serializer(),
            originalRepo.contains(key)
        )
    }

    get(countRoute) {
        call.unianswer(
            Long.serializer(),
            originalRepo.count()
        )
    }
}