package frontier.skc.match

import frontier.skc.util.displayName
import frontier.skc.value.AnnotatedValueCompleter
import frontier.skc.value.AnnotatedValueParameter
import frontier.skc.value.AnnotatedValueParser
import frontier.skc.value.AnnotatedValueUsage
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

class SKCMatcher {

    private val matches = arrayListOf<MatchedParameter>()

    fun match(match: ParameterMatch, parameter: AnnotatedValueParameter) {
        matches += MatchedParameter(match, parameter.parser, parameter.completer, parameter.usage)
    }

    fun parse(match: ParameterMatch, parser: AnnotatedValueParser) {
        matches += MatchedParameter(match, parser = parser)
    }

    fun complete(match: ParameterMatch, completer: AnnotatedValueCompleter) {
        matches += MatchedParameter(match, completer = completer)
    }

    fun usage(match: ParameterMatch, usage: AnnotatedValueUsage) {
        matches += MatchedParameter(match, usage = usage)
    }

    fun resolve(function: KFunction<*>): List<Pair<KParameter, AnnotatedValueParameter>> {
        val result = arrayListOf<Pair<KParameter, AnnotatedValueParameter>>()

        for (parameter in function.parameters) {
            var parser: AnnotatedValueParser? = null
            var completer: AnnotatedValueCompleter? = null
            var usage: AnnotatedValueUsage? = null

            for ((match, mParser, mCompleter, mUsage) in matches) {
                if (match(parameter)) {
                    parser = parser ?: mParser
                    completer = completer ?: mCompleter
                    usage = usage ?: mUsage
                }
            }

            if (parser == null)
                throw IllegalStateException("Unmatched parser for parameter '${parameter.displayName}: ${parameter.type}'")
            if (completer == null)
                throw IllegalStateException("Unmatched completer for parameter '${parameter.displayName}: ${parameter.type}'")
            if (usage == null)
                throw IllegalStateException("Unmatched usage for parameter '${parameter.displayName}: ${parameter.type}'")

            result += parameter to AnnotatedValueParameter(parser, completer, usage)
        }

        return result
    }

    private data class MatchedParameter(
        val match: ParameterMatch,
        val parser: AnnotatedValueParser? = null,
        val completer: AnnotatedValueCompleter? = null,
        val usage: AnnotatedValueUsage? = null
    )
}