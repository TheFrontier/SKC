package frontier.skc.match

import frontier.skc.util.display
import frontier.skc.value.*
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType

class SKCMatcher {

    private val matches = arrayListOf<MatchedParameter>()

    fun match(match: ParameterMatch, parameter: AnnotatedValueParameter) {
        matches += MatchedParameter(match,
            { _, _ -> parameter.parser },
            { _, _ -> parameter.completer },
            { _, _ -> parameter.usage })
    }

    fun parse(match: ParameterMatch, parser: AnnotatedValueParser) {
        matches += MatchedParameter(match, parser = { _, _ -> parser })
    }

    inline fun <reified T : Any> parseTyped(
        crossinline match: ParameterMatch,
        noinline parser: TypedAnnotatedValueParser<T>
    ) {
        parse(TypeMatch.onType<T>() and match, parser)
    }

    fun complete(match: ParameterMatch, completer: AnnotatedValueCompleter) {
        matches += MatchedParameter(match, completer = { _, _ -> completer })
    }

    fun usage(match: ParameterMatch, usage: AnnotatedValueUsage) {
        matches += MatchedParameter(match, usage = { _, _ -> usage })
    }

    fun parseComplex(match: ParameterMatch, parser: ParameterizedValueParser) {
        matches += MatchedParameter(match, parser = parser)
    }

    fun completeComplex(match: ParameterMatch, completer: ParameterizedValueCompleter) {
        matches += MatchedParameter(match, completer = completer)
    }

    fun usageComplex(match: ParameterMatch, usage: ParameterizedValueUsage) {
        matches += MatchedParameter(match, usage = usage)
    }

    fun findParser(type: KType, annotations: List<Annotation>): AnnotatedValueParser? {
        var parser: AnnotatedValueParser? = null

        for ((match, mParser) in matches) {
            if (mParser != null && match(type, annotations)) {
                val found = mParser(type, annotations)
                if (found != null) {
                    parser = found
                    break
                }
            }
        }
        return parser
    }

    fun findCompleter(type: KType, annotations: List<Annotation>): AnnotatedValueCompleter? {
        var completer: AnnotatedValueCompleter? = null

        for ((match, _, mCompleter) in matches) {
            if (mCompleter != null && match(type, annotations)) {
                val found = mCompleter(type, annotations)
                if (found != null) {
                    completer = found
                    break
                }
            }
        }
        return completer
    }

    fun findUsage(type: KType, annotations: List<Annotation>): AnnotatedValueUsage? {
        var usage: AnnotatedValueUsage? = null

        for ((match, _, _, mUsage) in matches) {
            if (mUsage != null && match(type, annotations)) {
                val found = mUsage(type, annotations)
                if (found != null) {
                    usage = found
                    break
                }
            }
        }
        return usage
    }

    fun resolve(type: KType, annotations: List<Annotation>): AnnotatedValueParameter {
        var parser: AnnotatedValueParser? = null
        var completer: AnnotatedValueCompleter? = null
        var usage: AnnotatedValueUsage? = null

        for ((match, mParser, mCompleter, mUsage) in matches) {
            if (match(type, annotations)) {
                parser = parser ?: mParser?.invoke(type, annotations)
                completer = completer ?: mCompleter?.invoke(type, annotations)
                usage = usage ?: mUsage?.invoke(type, annotations)
            }
        }

        if (parser == null)
            throw IllegalStateException("Unmatched parser for type $type with annotations ${annotations.display()}")
        if (completer == null)
            throw IllegalStateException("Unmatched completer for type $type with annotations ${annotations.display()}")
        if (usage == null)
            throw IllegalStateException("Unmatched usage for type $type with annotations ${annotations.display()}")

        return AnnotatedValueParameter(parser, completer, usage)
    }

    fun resolve(parameter: KParameter): AnnotatedValueParameter = resolve(parameter.type, parameter.annotations)

    fun resolve(function: KFunction<*>): List<Pair<KParameter, AnnotatedValueParameter>> {
        return function.parameters.map { it to resolve(it) }
    }

    private data class MatchedParameter(
        val match: ParameterMatch,
        val parser: ParameterizedValueParser? = null,
        val completer: ParameterizedValueCompleter? = null,
        val usage: ParameterizedValueUsage? = null
    )
}