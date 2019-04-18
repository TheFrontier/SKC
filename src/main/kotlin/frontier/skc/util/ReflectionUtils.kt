package frontier.skc.util

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.*

inline fun <reified A : Annotation> KClass<*>.annotatedFunctions(): List<KFunction<*>> =
    this.functions.filter { func -> func.annotations.any { it is A } }

inline fun <reified A : Annotation> KClass<*>.annotatedClasses(): List<KClass<*>> =
    this.nestedClasses.filter { clazz -> clazz.annotations.any { it is A } }

inline fun <reified T : Any> KClass<*>.isSubclassOf() = this.isSubclassOf(T::class)

inline fun <reified T : Any> KClass<*>.isSuperclassOf() = this.isSuperclassOf(T::class)

inline fun <reified T : Annotation> List<Annotation>.findAnnotation(): T? =
    this.firstOrNull { it is T } as T?

inline fun <reified T : Any> KType.isSubtypeOf() =
    this.isSubtypeOf(T::class.createType())

inline fun <reified T : Any> KType.isType() =
    this.classifier == T::class

val KParameter.displayName: String get() = this.name ?: "arg${this.index}"