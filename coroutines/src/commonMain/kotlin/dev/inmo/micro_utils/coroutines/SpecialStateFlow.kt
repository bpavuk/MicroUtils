package dev.inmo.micro_utils.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface SpecialMutableStateFlow<T> : StateFlow<T>, FlowCollector<T>, MutableSharedFlow<T> {
    open class Default<T>(
        initialValue: T,
        internalScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) : SpecialMutableStateFlow<T> {
        protected val internalSharedFlow: MutableSharedFlow<T> = MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        protected val publicSharedFlow: MutableSharedFlow<T> = MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        protected var _value: T = initialValue
        override val value: T
            get() = _value
        protected open suspend fun onChange(value: T) {
            _value = value
            publicSharedFlow.emit(value)
        }
        protected val job = internalSharedFlow.subscribeSafelyWithoutExceptions(internalScope) {
            if (_value != it) {
                onChange(it)
            }
        }

        override val replayCache: List<T>
            get() = publicSharedFlow.replayCache
        override val subscriptionCount: StateFlow<Int>
            get() = publicSharedFlow.subscriptionCount

        @ExperimentalCoroutinesApi
        override fun resetReplayCache() = publicSharedFlow.resetReplayCache()

        override fun tryEmit(value: T): Boolean {
            return internalSharedFlow.tryEmit(value)
        }

        override suspend fun emit(value: T) {
            internalSharedFlow.emit(value)
        }

        override suspend fun collect(collector: FlowCollector<T>) = publicSharedFlow.collect(collector)
    }
}
