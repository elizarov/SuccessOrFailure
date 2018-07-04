
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
    @PublishedApi internal val value: Any?
) : Serializable {
    // discovery

    public val isSuccess: Boolean get() = value !is Failure
    public val isFailure: Boolean get() = value is Failure

    // value retrieval

    public fun getOrThrow(): T =
        when (value) {
            is Failure -> throw value.exception
            else -> value as T
        }

    public fun getOrNull(): T? =
        when (value) {
            is Failure -> null
            else -> value as T
        }

    // exception retrieval

    public fun exceptionOrNull(): Throwable? =
        when (value) {
            is Failure -> value.exception
            else -> null
        }

    // identity

//    override fun equals(other: Any?): Boolean = other is SuccessOrFailure<*> && _value == other._value
// todo: workaround for is/as bugs
    override fun equals(other: Any?): Boolean {
        val other = other as? SuccessOrFailure<*> ?: return false
        return value == other.value
    }

    override fun hashCode(): Int = value?.hashCode() ?: 0
    override fun toString(): String = value.toString()

    // companion with constructors

    public companion object {
        @InlineOnly public inline fun <T> success(value: T): SuccessOrFailure<T> =
            SuccessOrFailure(value)

        @InlineOnly public inline fun <T> failure(exception: Throwable): SuccessOrFailure<T> =
            SuccessOrFailure(Failure(exception))
    }

    @PublishedApi
    internal class Failure @PublishedApi internal constructor(
        @JvmField
        val exception: Throwable
    ) : Serializable {
        override fun equals(other: Any?): Boolean = other is Failure && exception == other.exception
        override fun hashCode(): Int = exception.hashCode()
        override fun toString(): String = "Failure($exception)"
    }
}

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
    when(value) {
        is SuccessOrFailure.Failure -> defaultValue()
        else -> value as T
    }

// transformation

@InlineOnly public inline fun <R, T> SuccessOrFailure<T>.map(transform: (T) -> R): SuccessOrFailure<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when(value) {
        is SuccessOrFailure.Failure -> SuccessOrFailure(value) // cannot cast here -- casts don't work (todo)
        else -> SuccessOrFailure.success(transform(value as T))
    }
}

@InlineOnly public inline fun <R, T> SuccessOrFailure<T>.mapCatching(transform: (T) -> R): SuccessOrFailure<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when(value) {
        is SuccessOrFailure.Failure -> SuccessOrFailure(value) // cannot cast here -- casts don't work (todo)
        else -> runCatching { transform(value as T) }
    }
}

@InlineOnly public inline fun <R, T: R> SuccessOrFailure<T>.recover(transform: (Throwable) -> R): SuccessOrFailure<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when(value) {
        is SuccessOrFailure.Failure -> SuccessOrFailure.success(transform(value.exception))
        else -> this
    }
}

@InlineOnly public inline fun <R, T: R> SuccessOrFailure<T>.recoverCatching(transform: (Throwable) -> R): SuccessOrFailure<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    val value = value // workaround for inline classes BE bug
    return when(value) {
        is SuccessOrFailure.Failure -> runCatching { transform(value.exception) }
        else -> this
    }
}

// "peek" onto value/exception and pipe

@InlineOnly public inline fun <T> SuccessOrFailure<T>.onFailure(block: (Throwable) -> Unit): SuccessOrFailure<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (value is SuccessOrFailure.Failure) block(value.exception)
    return this
}

@InlineOnly public inline fun <T> SuccessOrFailure<T>.onSuccess(block: (T) -> Unit): SuccessOrFailure<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (value !is SuccessOrFailure.Failure) block(value as T)
    return this
}

// -------------------