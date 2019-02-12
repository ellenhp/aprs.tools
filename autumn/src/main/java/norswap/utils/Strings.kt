package norswap.utils

// -------------------------------------------------------------------------------------------------

/**
 * Converts this alphanum CamelCase string to snake_case.
 * @author http://stackoverflow.com/questions/10310321
 */
fun String.camel_to_snake(): String
{
    return this
        .replace("([a-z0-9])([A-Z]+)".toRegex(), "$1_$2")
        .toLowerCase()
}

// -------------------------------------------------------------------------------------------------

/**
 * Converts this alphanum snake_case string to CamelCase.
 */
fun String.snake_to_camel(): String
{
    return this
        .split('_')
        .map(String::capitalize)
        .joinToString("")
}

// -------------------------------------------------------------------------------------------------