package frontier.skc.util

import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

inline fun <reified T : Any> KType.isSubtypeOf() =
    this.isSubtypeOf(T::class.createType())

inline fun <reified T : Any> KType.isType() =
    this.classifier == T::class