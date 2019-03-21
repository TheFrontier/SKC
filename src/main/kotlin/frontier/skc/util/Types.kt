package frontier.skc.util

import org.spongepowered.api.world.Location
import org.spongepowered.api.world.World
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType

val Location_World: KType =
    Location::class.createType(arguments = listOf(KTypeProjection.covariant(World::class.createType())))

val Enum_STAR: KType = Enum::class.starProjectedType