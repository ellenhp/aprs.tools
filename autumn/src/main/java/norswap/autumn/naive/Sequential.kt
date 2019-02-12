package norswap.autumn.naive
import norswap.autumn.parsers.*

// -------------------------------------------------------------------------------------------------

/**
 * Matches all the parsers in a sequence.
 */
class Seq (vararg val ps: Parser): Parser()
{
    override fun invoke() = grammar.seq { ps.all(Parser::invoke) }
}

// -------------------------------------------------------------------------------------------------

/**
 * Matches [p] if it suceeds, otherwise succeeds without consuming any input.
 */
class Opt (val p: Parser): Parser()
{
    override fun invoke() = grammar.opt(p)
}

// -------------------------------------------------------------------------------------------------

/**
 * Matches 0 or more (sequential) repetition of [p].
 */
class Repeat0 (val p: Parser): Parser()
{
    override fun invoke() = grammar.repeat0(p)
}

// -------------------------------------------------------------------------------------------------

/**
 * Matches 1 or more (sequential) repetition of [p].
 */
class Repeat1 (val p: Parser): Parser()
{
    override fun invoke() = grammar.repeat1(p)
}

// -------------------------------------------------------------------------------------------------

/**
 * Matches exactly [n] (sequential) repetitions of [p].
 */
class Repeat (val n: Int, val p: Parser): Parser()
{
    override fun invoke() = grammar.repeat(n, p)
}

// -------------------------------------------------------------------------------------------------

/**
 * Matches 0 or more repetitions of [around], separated from one another by input matching [inside].
 */
class Around0 (val around: Parser, val inside: Parser): Parser()
{
    override fun invoke() = grammar.around0(around, inside)
}

// -------------------------------------------------------------------------------------------------

/**
 * Matches 1 or more repetitions of [around], separated from one another by input matching [inside].
 */
class Around1 (val around: Parser, val inside: Parser): Parser()
{
    override fun invoke() = grammar.around1(around, inside)
}

// -------------------------------------------------------------------------------------------------

/**
 * Matches 0 or more repetitions of [around], separated from one another by input matching [inside],
 * optionally followed by input matching [inside].
 */
class ListTerm0 (val around: Parser, val inside: Parser): Parser()
{
    override fun invoke() = grammar.list_term0(around, inside)
}

// -------------------------------------------------------------------------------------------------

/**
 * Matches 1 or more repetitions of [around], separated from one another by input matching [inside],
 * optionally followed by input matching [inside].
 */
class ListTerm1 (val around: Parser, val inside: Parser): Parser()
{
    override fun invoke() = grammar.list_term1(around, inside)
}

// -------------------------------------------------------------------------------------------------

/**
 * Matches 0 or more repetition of [repeat], followed by [terminator].
 *
 * In case of ambiguity, [terminator] is matched in preference to [repeat]
 * (this is what makes this different from `seq { repeat0(repeat) && terminator() }`).
 */
class Until0 (val repeat: Parser, val terminator: Parser): Parser()
{
    override fun invoke() = grammar.until0(repeat, terminator)
}

// -------------------------------------------------------------------------------------------------

/**
 * Matches 1 or more repetition of [repeat], followed by [terminator].
 *
 * In case of ambiguity, [terminator] is matched in preference to [repeat]
 * (this is what makes this different from `seq { repeat1(repeat) && terminator() }`).
 */
class Until1 (val repeat: Parser, val terminator: Parser): Parser()
{
    override fun invoke() = grammar.until1(repeat, terminator)
}

// -------------------------------------------------------------------------------------------------