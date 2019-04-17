package frontier.skc.match

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

object AnnotationMatch {

    private val EMPTY: ParameterMatch = { it.annotations.isEmpty() }

    fun onEmpty(): ParameterMatch = EMPTY

    fun onAnnotation(type: KClass<out Annotation>): ParameterMatch =
        { parameter -> parameter.annotations.any { it::class == type } }

    inline fun <reified A : Annotation> onAnnotation(
        type: KClass<out A>,
        crossinline filter: (A) -> Boolean
    ): ParameterMatch =
        { parameter -> (parameter.annotations.find { it::class == type } as A?)?.let(filter) == true }

    inline fun <reified A : Annotation> onAnnotation(): ParameterMatch =
        { parameter -> parameter.annotations.any { it is A } }

    inline fun <reified A : Annotation> onAnnotation(crossinline filter: (A) -> Boolean): ParameterMatch =
        { parameter -> parameter.findAnnotation<A>()?.let(filter) == true }
}