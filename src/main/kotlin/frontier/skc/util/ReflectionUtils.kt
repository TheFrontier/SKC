package frontier.skc.util

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSuperclassOf

inline fun <reified T : Any> KClass<*>.isSubclassOf() = this.isSubclassOf(T::class)

inline fun <reified T : Any> KClass<*>.isSuperclassOf() = this.isSuperclassOf(T::class)

inline fun <reified T : Annotation> List<Annotation>.findAnnotation(): T? =
    this.firstOrNull { it is T } as T?

inline fun <reified T : Any> KType.isSubtypeOf() =
    this.isSubtypeOf(T::class.createType())

inline fun <reified T : Any> KType.isType() =
    this.classifier == T::class

val KParameter.effectiveName: String get() = this.name ?: "arg${this.index}"