package norswap.utils

// -------------------------------------------------------------------------------------------------

/**
 * Similar to [associate] but ignores keys for which [f] returns null.
 */
inline fun <T, K, V> Iterable<T>.assoc_not_null (f: (T) -> Pair<K, V>?): Map<K, V>
{
    val out = HashMap<K, V>()
    out.putAll(mapNotNull(f))
    return out
}

// -------------------------------------------------------------------------------------------------

/**
 * Similar to [associate] but ignores keys for which [f] returns null.
 */
inline fun <T, K, V> Array<T>.assoc_not_null (f: (T) -> Pair<K, V>?): Map<K, V>
{
    val out = HashMap<K, V>()
    out.putAll(mapNotNull(f))
    return out
}

// ------------------------------------------------------------------------------------------------