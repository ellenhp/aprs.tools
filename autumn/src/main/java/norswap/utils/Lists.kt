package norswap.utils

// -------------------------------------------------------------------------------------------------

/**
 * Like `this.flatten.forEach(f)`, but without the memory overheads.
 */
inline fun <T> Iterable<Iterable<T>>.flat_foreach (f: (T) -> Unit) {
    forEach { it.forEach { f(it) } }
}

// -------------------------------------------------------------------------------------------------

/**
 * Returns of first instance of [T] from the iterable, or throws an exception.
 */
inline fun <reified T> Iterable<*>.first_instance(): T
    = filter { it is T } as T

// -------------------------------------------------------------------------------------------------

/**
 * Prints the list with each item on a single line
 */
fun List<*>.lines(): String
    =  joinToString(separator = ",\n ", prefix = "[", postfix = "]")

// -------------------------------------------------------------------------------------------------

/**
 * Returns a view of the list without its first [n] items (default: 1).
 */
fun <T> List<T>.rest (n: Int = 1): List<T>
    = subList(n, size)

// -------------------------------------------------------------------------------------------------

/**
 * Returns a view of the list without its last [n] items (default: 1).
 */
fun <T> List<T>.except (n: Int = 1): List<T>
    = subList(0, size - n)

// -------------------------------------------------------------------------------------------------

/**
 * Returns a list wrapping [item] if not null or an empty list otherwise.
 */
fun <T: Any> maybe_list (item: T?): List<T>
    = if (item == null) emptyList() else listOf(item)

// -------------------------------------------------------------------------------------------------

/**
 * Returns [list] if not null or an empty list otherwise.
 */
fun <T> maybe_list (list: List<T>?): List<T>
    = list ?: emptyList()

// -------------------------------------------------------------------------------------------------