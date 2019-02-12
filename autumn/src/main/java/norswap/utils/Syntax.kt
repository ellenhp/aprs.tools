package norswap.utils

// -------------------------------------------------------------------------------------------------

/**
 * Shorthand for [StringBuilder.append].
 */
operator fun StringBuilder.plusAssign(o: Any?) { append(o) }

// -------------------------------------------------------------------------------------------------

/**
 * Syntactic sugar for `if (this) f() else null`
 *
 * Enables two cute things:
 *
 * - An if that evaluates to true on the else branch: `(<condition>) .. { <body> }`
 * - The ternary operator: `(<condition>) .. { <if-true> } ?: <if-false>`
 */
inline operator fun <T> Boolean.rangeTo (f: () -> T): T?
    = then(f)

// -------------------------------------------------------------------------------------------------

/**
 * Syntactic sugar for `if (this) f() else null`
 *
 * Enables two cute things:
 *
 * - An if that evaluates to true on the else branch: `(<condition>) then { <body> }`
 * - The ternary operator: `(<condition>) then { <if-true> } ?: <if-false>`
 */
inline infix fun <T> Boolean.then (f: () -> T): T?
    = if (this) f() else null

// -------------------------------------------------------------------------------------------------

/**
 * Casts the receiver to type T (unsafe). T can be automatically inferred.
 */
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Any?.cast(): T
    = this as T

// -------------------------------------------------------------------------------------------------

/**
 * Use this to enable Kotlin smart casts at no cost: include a value cast as the parameter to
 * this function, and the code that follows will be able to assume that the value is of the
 * type it was casted to.
 *
 * e.g. `proclaim (vehicle as Truck)`
 */
@Suppress("UNUSED_PARAMETER", "NOTHING_TO_INLINE")
inline fun proclaim (cast: Any)
    = Unit

// -------------------------------------------------------------------------------------------------


/**
 * If the receiver can be cast to [T], run [f] over it and return the result. Else return null.
 *
 * You typically have to specify the type of the parameter in the lambda, because Kotlin type
 * inference is dumb.
 */
inline fun <reified T, R> Any?.when_is (f: (T) -> R): R?
    = if (this is T) f(this) else null

// -------------------------------------------------------------------------------------------------

/**
 * Applies [f] to the receiver if it is non-null, else return null.
 */
inline fun <T: Any, R: Any> T?.inn (f: (T) -> R): R?
    = if (this != null) f(this) else null

// -------------------------------------------------------------------------------------------------

/**
 * Tries to run [f], returning its return value if successful and null if an exception is thrown.
 */
inline fun <T: Any> attempt (f: () -> T): T?
    = try { f() } catch (_: Exception) { null }

// -------------------------------------------------------------------------------------------------