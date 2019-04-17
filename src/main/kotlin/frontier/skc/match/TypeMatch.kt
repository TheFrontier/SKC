package frontier.skc.match

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSupertypeOf

object TypeMatch {

    fun onType(type: KType): ParameterMatch = { it.type == type }

    fun onType(type: KClass<*>): ParameterMatch = onType(type.createType())

    inline fun <reified T : Any> onType(): ParameterMatch = onType(T::class.createType())

    fun onSubtype(type: KType): ParameterMatch = { it.type.isSubtypeOf(type) }

    fun onSubtype(type: KClass<*>): ParameterMatch = onSubtype(type.createType())

    inline fun <reified T : Any> onSubtype(): ParameterMatch = onSubtype(T::class.createType())

    fun onSupertype(type: KType): ParameterMatch = { it.type.isSupertypeOf(type) }

    fun onSupertype(type: KClass<*>): ParameterMatch = onSupertype(type.createType())

    inline fun <reified T : Any> onSupertype(): ParameterMatch = onSupertype(T::class.createType())
}