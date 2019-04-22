package frontier.skc.match

import frontier.skc.value.AnnotatedValueCompleter
import frontier.skc.value.AnnotatedValueParser
import frontier.skc.value.AnnotatedValueUsage
import kotlin.reflect.KType

typealias ParameterMatch = (type: KType, annotations: List<Annotation>) -> Boolean

typealias ParameterizedValueParser = (type: KType, annotations: List<Annotation>) -> AnnotatedValueParser?

typealias ParameterizedValueCompleter = (type: KType, annotations: List<Annotation>) -> AnnotatedValueCompleter?

typealias ParameterizedValueUsage = (type: KType, annotations: List<Annotation>) -> AnnotatedValueUsage?