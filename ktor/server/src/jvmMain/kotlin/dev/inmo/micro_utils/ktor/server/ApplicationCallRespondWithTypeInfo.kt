package dev.inmo.micro_utils.ktor.server

import io.ktor.server.application.ApplicationCall
import io.ktor.server.http.*
import io.ktor.server.response.responseType
import io.ktor.utils.io.InternalAPI
import io.ktor.util.reflect.TypeInfo

@InternalAPI
suspend fun <T : Any> ApplicationCall.respond(
    message: T,
    typeInfo: TypeInfo
) {
    response.responseType = typeInfo
    respond(message, typeInfo)
}
