package norswap.utils
import java.lang.reflect.ParameterizedType

// -------------------------------------------------------------------------------------------------

/**
 * True iff the receiver is a subtype of (i.e. is assignable to) the parameter.
 */
infix fun Class<*>.extends(other: Class<*>): Boolean
    = other.isAssignableFrom(this)

// -------------------------------------------------------------------------------------------------

/**
 * True iff the receiver is a supertype of (i.e. can be assigned from) the parameter.
 */
infix fun Class<*>.supers (other: Class<*>): Boolean
    = this.isAssignableFrom(other)

// -------------------------------------------------------------------------------------------------

/**
 * True iff the type parameter is a subtype of (i.e. is assignable to) the parameter.
 */
inline fun <reified T: Any> extends (other: Class<*>): Boolean
    = T::class.java extends other

// -------------------------------------------------------------------------------------------------

/**
 * True iff the type parameter is a supertype of (i.e. can be assigned from) the parameter.
 */
inline fun <reified T: Any> supers (other: Class<*>): Boolean
    = T::class.java supers other

// -------------------------------------------------------------------------------------------------

/**
 * Returns the nth type argument to the superclass of the class of [obj].
 */
fun nth_superclass_targ (obj: Any, n: Int): Class<*>?
{
    val zuper = obj::class.java.genericSuperclass as? ParameterizedType ?: return null
    if (zuper.actualTypeArguments.size < n) return null
    val ntype = zuper.actualTypeArguments[n - 1]
    if (ntype is ParameterizedType)
        return ntype.rawType as Class<*>
    else
        return ntype as Class<*>
}

// -------------------------------------------------------------------------------------------------