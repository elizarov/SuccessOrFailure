
@file:Suppress(
    "UNCHECKED_CAST",
    "RedundantVisibilityModifier",
    "NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS",
    "UNSUPPORTED_FEATURE",
    "INVISIBLE_REFERENCE",
    "INVISIBLE_MEMBER"
)

import kotlin.internal.InlineOnly
import kotlin.jvm.JvmField

public inline class SuccessOrFailure<out T> @PublishedApi internal constructor(private val _value: Any?) {
    // discovery

    public val isSuccess: Boolean get() = _value !is Failure
    public val isFailure: Boolean get() = _value is Failure

    // value retrieval

    public fun getOrThrow(): T = when (_value) {
        is Failure -> throw _value.exception
        else -> _value as T
    }

    public fun getOrNull(): T? = when (_value) {
        is Failure -> null
        else -> _value as T
    }

    // exception retrieval

    public fun exceptionOrNull(): Throwable? = when (_value) {
        is Failure -> _value.exception
        else -> null
    }

    // internal API for inline functions

    @PublishedApi internal val exception: Throwable get() = (_value as Failure).exception
    @PublishedApi internal val value: T get() = _value as T

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

@InlineOnly public inline fun <R> runCatching(block: () -> R): SuccessOrFailure<R> =
    try {
        SuccessOrFailure.success(block())
    } catch (e: Throwable) {
        SuccessOrFailure.failure(e)
    }

@InlineOnly public inline fun <T, R> T.runCatching(block: T.() -> R): SuccessOrFailure<R> =
    try {
        SuccessOrFailure.success(block())
    } catch (e: Throwable) {
        SuccessOrFailure.failure(e)
    }

// -- extensions ---

@InlineOnly public inline fun <R, T : R> SuccessOrFailure<T>.getOrElse(defaultValue: () -> R): R = when {
    isFailure -> defaultValue()
    else -> value
}

// transformation

@InlineOnly public inline fun <R, T> SuccessOrFailure<T>.map(transform: (T) -> R): SuccessOrFailure<R> =
    if (isFailure) this as SuccessOrFailure<R>
    else SuccessOrFailure.success(transform(value))

@InlineOnly public inline fun <R, T> SuccessOrFailure<T>.mapCatching(transform: (T) -> R): SuccessOrFailure<R> =
    if (isFailure) this as SuccessOrFailure<R>
    else runCatching { transform(value) }

@InlineOnly public inline fun <R, T: R> SuccessOrFailure<T>.recover(transform: (Throwable) -> R): SuccessOrFailure<R> =
    if (isFailure) SuccessOrFailure.success(transform(exception))
    else this

@InlineOnly public inline fun <R, T: R> SuccessOrFailure<T>.recoverCatching(transform: (Throwable) -> R): SuccessOrFailure<R> =
    if (isFailure) runCatching { transform(exception) }
    else this

// "peek" onto value/exception and pipe

@InlineOnly public inline fun <T> SuccessOrFailure<T>.onFailure(block: (Throwable) -> Unit): SuccessOrFailure<T> {
    if (isFailure) block(exception)
    return this
}

@InlineOnly public inline fun <T> SuccessOrFailure<T>.onSuccess(block: (T) -> Unit): SuccessOrFailure<T> {
    if (isSuccess) block(value)
    return this
}

// -------------------