
@file:Suppress(
    "UNCHECKED_CAST",
    "RedundantVisibilityModifier",
    "NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS",
    "UNSUPPORTED_FEATURE",
    "INVISIBLE_REFERENCE",
    "INVISIBLE_MEMBER"
)

import kotlin.internal.InlineOnly
import kotlin.internal.contracts.*
import kotlin.jvm.JvmField

public inline class SuccessOrFailure<out T> @PublishedApi internal constructor(
    @PublishedApi internal val _value: Any?
) : java.io.Serializable {
    // discovery

    public val isSuccess: Boolean get() = _value !is Failure
    public val isFailure: Boolean get() = _value is Failure

    // value retrieval

    public fun getOrThrow(): T =
        when (_value) {
            is Failure -> throw _value.exception
            else -> _value as T
        }

    public fun getOrNull(): T? =
        when (_value) {
            is Failure -> null
            else -> _value as T
        }

    // exception retrieval

    public fun exceptionOrNull(): Throwable? =
        when (_value) {
            is Failure -> _value.exception
            else -> null
        }

    // identity

// todo:
//    fun equals(other: SuccessOrFailure<*>): Boolean = _value == other._value
    override fun hashCode(): Int = _value?.hashCode() ?: 0
    override fun toString(): String = _value.toString()

    // companion with constructors

    public companion object {
        @InlineOnly public inline fun <T> success(value: T): SuccessOrFailure<T> =
            SuccessOrFailure(value)

        @InlineOnly public inline fun <T> failure(exception: Throwable): SuccessOrFailure<T> =
            SuccessOrFailure(Failure(exception))
    }
}

// top-Level internal failure-marker class
// todo: maybe move it to another kotlin.internal package?
@PublishedApi
internal class Failure @PublishedApi internal constructor(
    @JvmField
    val exception: Throwable
) : Serializable

@InlineOnly public inline fun <R> runCatching(block: () -> R): SuccessOrFailure<R> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return try {
        SuccessOrFailure.success(block())
    } catch (e: Throwable) {
        SuccessOrFailure.failure(e)
    }
}

@InlineOnly public inline fun <T, R> T.runCatching(block: T.() -> R): SuccessOrFailure<R> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return try {
        SuccessOrFailure.success(block())
    } catch (e: Throwable) {
        SuccessOrFailure.failure(e)
    }
}

// -- extensions ---

@InlineOnly public inline fun <R, T : R> SuccessOrFailure<T>.getOrElse(defaultValue: () -> R): R =
    when(_value) {
        is Failure -> defaultValue()
        else -> _value as T
    }

// transformation

@InlineOnly public inline fun <R, T> SuccessOrFailure<T>.map(transform: (T) -> R): SuccessOrFailure<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when(_value) {
        is Failure -> SuccessOrFailure(_value)
        else -> SuccessOrFailure.success(transform(_value as T))
    }
}

@InlineOnly public inline fun <R, T> SuccessOrFailure<T>.mapCatching(transform: (T) -> R): SuccessOrFailure<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when(_value) {
        is Failure -> SuccessOrFailure(_value)
        else -> runCatching { transform(_value as T) }
    }
}

@InlineOnly public inline fun <R, T: R> SuccessOrFailure<T>.recover(transform: (Throwable) -> R): SuccessOrFailure<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when(_value) {
        is Failure -> SuccessOrFailure.success(transform(_value.exception))
        else -> this
    }
}

@InlineOnly public inline fun <R, T: R> SuccessOrFailure<T>.recoverCatching(transform: (Throwable) -> R): SuccessOrFailure<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when(_value) {
        is Failure -> runCatching { transform((_value as Failure).exception) }
        else -> this
    }
}

// "peek" onto value/exception and pipe

@InlineOnly public inline fun <T> SuccessOrFailure<T>.onFailure(block: (Throwable) -> Unit): SuccessOrFailure<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (_value is Failure) block(_value.exception)
    return this
}

@InlineOnly public inline fun <T> SuccessOrFailure<T>.onSuccess(block: (T) -> Unit): SuccessOrFailure<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (_value !is Failure) block(_value as T)
    return this
}

// -------------------