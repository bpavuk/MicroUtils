package dev.inmo.micro_utils.coroutines

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun CoroutineScope.launchSafely(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    onException: ExceptionHandler<Unit> = defaultSafelyExceptionHandler,
    block: suspend CoroutineScope.() -> Unit
) = launch(context, start) {
    safely(onException, block)
}

fun CoroutineScope.launchSafelyWithoutExceptions(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    onException: ExceptionHandler<Unit?> = defaultSafelyWithoutExceptionHandlerWithNull,
    block: suspend CoroutineScope.() -> Unit
) = launch(context, start) {
    safelyWithoutExceptions(onException, block)
}

fun <T> CoroutineScope.asyncSafely(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    onException: ExceptionHandler<T> = defaultSafelyExceptionHandler,
    block: suspend CoroutineScope.() -> T
) = async(context, start) {
    safely(onException, block)
}

fun <T> CoroutineScope.asyncSafelyWithoutExceptions(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    onException: ExceptionHandler<T?> = defaultSafelyWithoutExceptionHandlerWithNull,
    block: suspend CoroutineScope.() -> T
) = async(context, start) {
    safelyWithoutExceptions(onException, block)
}
