package norswap.utils
import java.util.Arrays

// -------------------------------------------------------------------------------------------------

/**
 * Shorthand for [Arrays.toString].
 */
inline val <T> Array<T>.str: String
    get() = Arrays.toString(this)


// -------------------------------------------------------------------------------------------------

/**
 * Returns an array of the given size, populated with nulls, but casted to an array of non-nullable
 * items. This is unsafe, but handy when an array has to be allocated just to be populated
 * immediately, but the map-style [Array] constructor is not convenient. It also helps construct
 * array of nulls for non-reifiable types.
 */
fun <T> arrayOfSize (size: Int): Array<T>
{
    @Suppress("UNCHECKED_CAST")
    return arrayOfNulls<Any>(size) as Array<T>
}

// -------------------------------------------------------------------------------------------------

/**
 * Inexplicably missing standard library function.
 */
fun <T> Sequence<T>.toArray(): Array<T>
{
    @Suppress("UNCHECKED_CAST")
    return toCollection(ArrayList<T>()).toArray() as Array<T>
}

// -------------------------------------------------------------------------------------------------

/**
 * Maps a sequence to an array.
 */
inline fun <T, Out> Sequence<T>.mapToArray (f: (T) -> Out): Array<Out>
{
    @Suppress("UNCHECKED_CAST")
    return mapTo(ArrayList<Out>(), f).toArray() as Array<Out>
}

// -------------------------------------------------------------------------------------------------

/**
 * Maps a list to an array.
 */
inline fun <T, Out> List<T>.mapToArray(f: (T) -> Out): Array<Out>
{
    @Suppress("UNCHECKED_CAST")
    return mapTo(ArrayList<Out>(), f).toArray() as Array<Out>
}

// -------------------------------------------------------------------------------------------------