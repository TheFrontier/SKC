package frontier.skc.match

import frontier.skc.util.findAnnotation
import kotlin.reflect.KClass

object AnnotationMatch {

    private val EMPTY: ParameterMatch = { _, annotations -> annotations.isEmpty() }

    fun onEmpty(): ParameterMatch = EMPTY

    fun onAnnotation(type: KClass<out Annotation>): ParameterMatch =
        { _, annotations -> annotations.any { it::class == type } }

    inline fun <reified A : Annotation> onAnnotation(
        type: KClass<out A>,
        crossinline filter: (A) -> Boolean
    ): ParameterMatch =
        { _, annotations -> (annotations.find { it::class == type } as A?)?.let(filter) == true }

    inline fun <reified A : Annotation> onAnnotation(): ParameterMatch =
        { _, annotations -> annotations.any { it is A } }

    inline fun <reified A : Annotation> onAnnotation(crossinline filter: (A) -> Boolean): ParameterMatch =
        { _, annotations -> annotations.findAnnotation<A>()?.let(filter) == true }
}