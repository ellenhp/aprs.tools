@file:Suppress("PackageDirectoryMismatch")
package norswap.utils.multimap

import norswap.utils.inn

// -------------------------------------------------------------------------------------------------
/*

A set of utilities to emulate multi-maps (maps where multiple values can be associated to each key)
in terms of maps from keys to arraylists of values.

The idea is to avoid redefining a whole class and to exploit the flexibility of the underlying
representation.

*/
// -------------------------------------------------------------------------------------------------

typealias MultiMap<K, V>        = Map<K, List<V>>
typealias MutableMultiMap<K, V> = MutableMap<K, ArrayList<V>>
typealias HashMultiMap<K, V>    = HashMap<K, ArrayList<V>>

// -------------------------------------------------------------------------------------------------

/**
 * Returns an empty immutable multimap.
 */
fun <K, V> empty_multimap(): MultiMap<K, V>
    = emptyMap()

// -------------------------------------------------------------------------------------------------

/**
 * Returns the value (list) associated with the key, or an empty list (which is not added to the
 * map! -- for that use [get_or_create]).
 */
fun <K, V> MultiMap<K, V>.get_or_empty(k: K): List<V>
    = get(k) ?: emptyList()

// -------------------------------------------------------------------------------------------------

/**
 * If the key doesn't have a value (list) yet, inserts a list with the value, otherwise appends
 * the value to the list.
 */
fun <K, V> MutableMultiMap<K, V>.append (k: K, v: V)
{
    var array = this[k]

    if (array == null) {
        array = ArrayList()
        put(k, array)
    }

    array.add(v)
}

// -------------------------------------------------------------------------------------------------

/**
 * Associates all values in [vs] with the key [k].
 */
fun <K, V> MutableMultiMap<K, V>.append (k: K, vs: Iterable<V>) {
    get_or_create(k).addAll(vs)
}

// -------------------------------------------------------------------------------------------------

/**
 * Returns the value (list) associated to the key, or associate it an empty list and return it.
 */
fun <K, V> MutableMultiMap<K, V>.get_or_create(k: K): ArrayList<V>
    = getOrPut(k) { ArrayList() }

// -------------------------------------------------------------------------------------------------

/**
 * Remove the given key value pair from the map. Returns true if the item was contained in the map.
 */
fun <K, V> MutableMultiMap<K, V>.remove (k: K, v: V): Boolean
    = get(k)?.remove(v) ?: false

// -------------------------------------------------------------------------------------------------

/**
 * Adds all key value pairs from [map] into this multi-map.
 */
fun <K, V> MutableMultiMap<K, V>.putSingles (map: Map<K, V>) {
    map.forEach { k, v -> append(k, v) }
}

// -------------------------------------------------------------------------------------------------

/**
 * Adds the key-value pairs of [other] into this multi-map.
 */
fun <K, V> MutableMultiMap<K, V>.putAll (other: MultiMap<K, V>) {
    other.forEach { k, vs -> append(k, vs) }
}

// -------------------------------------------------------------------------------------------------

/**
 * Add all key value pairs derived from the iterable by [f] to this multi-map.
 */
fun <X, K, V> Iterable<X>.multi_assoc (f: (X) -> Pair<K, V>): HashMultiMap<K, V>
{
    val out = HashMultiMap<K, V>()
    forEach { val (k, v) = f(it) ; out.append(k, v) }
    return out
}

// -------------------------------------------------------------------------------------------------

/**
 * Add all key value pairs derived from the array by [f] to this multi-map.
 */
fun <X, K, V> Array<X>.multi_assoc (f: (X) -> Pair<K, V>): HashMultiMap<K, V>
{
    val out = HashMultiMap<K, V>()
    forEach { val (k, v) = f(it) ; out.append(k, v) }
    return out
}

// -------------------------------------------------------------------------------------------------

/**
 * Add all key value pairs derived from the iterable by [f] to this multi-map.
 */
fun <X, K, V> Iterable<X>.multi_assoc_not_null (f: (X) -> Pair<K, V>?): HashMultiMap<K, V>
{
    val out = HashMultiMap<K, V>()
    forEach { f(it).inn { (k, v) -> out.append(k, v) } }
    return out
}

// -------------------------------------------------------------------------------------------------

/**
 * Add all key value pairs derived from the array by [f] to this multi-map.
 */
fun <X, K, V> Array<X>.multi_assoc_not_null (f: (X) -> Pair<K, V>?): HashMultiMap<K, V>
{
    val out = HashMultiMap<K, V>()
    forEach { f(it).inn { (k, v) -> out.append(k, v) } }
    return out
}

// -------------------------------------------------------------------------------------------------

/**
 * Returns all values in the multi-map.
 */
fun <K, V> MultiMap<K, V>.flat_values(): List<V>
    = entries.flatMap { it.value }

// -------------------------------------------------------------------------------------------------

/**
 * Returns all entries in the multi-map.
 */
fun <K, V> MultiMap<K, V>.flat_entries(): List<Pair<K, V>>
    = entries.flatMap { e -> e.value.map { e.key to it } }

// -------------------------------------------------------------------------------------------------

/**
 * Inserts all the entries int a new mutable multimap
 */
fun <K, V> Iterable<Pair<K, V>>.to_mutable_multimap(): MutableMultiMap<K, V>
{
    val map = HashMultiMap<K, V>()
    forEach { map.append(it.first, it.second) }
    return map
}

// -------------------------------------------------------------------------------------------------

/**
 * Inserts all the entries int a new multimap.
 */
fun <K, V> Iterable<Pair<K, V>>.to_multimap(): MultiMap<K, V>
    = to_mutable_multimap()

// -------------------------------------------------------------------------------------------------

/**
 * Returns a map containing all key-value pairs with values matching the given predicate.
 */
fun <K, V> MultiMap<K, V>.flat_filter_values (pred: (V) -> Boolean): MultiMap<K, V>
    = flat_entries()
        .filter { pred(it.second) }
        .to_multimap()

// -------------------------------------------------------------------------------------------------