package frontier.skc

import com.flowpowered.math.vector.Vector3d
import frontier.skc.annotation.OrSource
import frontier.skc.annotation.RemainingJoined
import frontier.skc.annotation.Serialized
import frontier.skc.annotation.Source
import frontier.skc.util.CommandSourceCommandElement
import frontier.skc.util.directlyMatchOnType
import frontier.skc.util.isSubtypeOf
import frontier.skc.util.matchOnType
import frontier.ske.gameRegistry
import frontier.ske.getType
import org.spongepowered.api.CatalogType
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandElement
import org.spongepowered.api.command.args.GenericArguments
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.entity.living.player.User
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.serializer.TextSerializer
import org.spongepowered.api.text.serializer.TextSerializers
import org.spongepowered.api.world.DimensionType
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.World
import org.spongepowered.api.world.storage.WorldProperties
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

object ParameterMappings {

    val PLAYER: ParameterMapping = matchOnType<Player> {
        when {
            it.findAnnotation<OrSource>() != null -> GenericArguments::playerOrSource
            else -> GenericArguments::player
        }
    }

    val USER: ParameterMapping = matchOnType<User> {
        when {
            it.findAnnotation<OrSource>() != null -> GenericArguments::userOrSource
            else -> GenericArguments::user
        }
    }

    val STRING: ParameterMapping = matchOnType<String> {
        when (it.findAnnotation<RemainingJoined>()?.raw) {
            true -> GenericArguments::remainingRawJoinedStrings
            false -> GenericArguments::remainingJoinedStrings
            else -> GenericArguments::string
        }
    }

    val BOOLEAN: ParameterMapping = directlyMatchOnType<Boolean>(GenericArguments::bool)

    val INT: ParameterMapping = directlyMatchOnType<Int>(GenericArguments::integer)

    val LONG: ParameterMapping = directlyMatchOnType<Long>(GenericArguments::longNum)

    val DOUBLE: ParameterMapping = directlyMatchOnType<Float>(GenericArguments::doubleNum)

    val BIGINTEGER: ParameterMapping = directlyMatchOnType<BigInteger>(GenericArguments::bigInteger)

    val BIGDECIMAL: ParameterMapping = directlyMatchOnType<BigDecimal>(GenericArguments::bigDecimal)

    val LOCATION: ParameterMapping = directlyMatchOnType<Location<World>>(GenericArguments::location)

    val VECTOR3D: ParameterMapping = directlyMatchOnType<Vector3d>(GenericArguments::vector3d)

    val WORLD: ParameterMapping = directlyMatchOnType<WorldProperties>(GenericArguments::world)

    val DIMENSION: ParameterMapping = directlyMatchOnType<DimensionType>(GenericArguments::dimension)

    val PLUGIN: ParameterMapping = directlyMatchOnType<PluginContainer>(GenericArguments::plugin)

    val TEXT: ParameterMapping = matchOnType<Text> {
        val serializer = it.findAnnotation<Serialized>()?.let { serialized ->
            gameRegistry.getType<TextSerializer>(serialized.id)
        } ?: TextSerializers.FORMATTING_CODE

        val allRemaining = it.findAnnotation<RemainingJoined>()

        return@matchOnType { key ->
            GenericArguments.text(key, serializer, allRemaining != null)
        }
    }

    @Suppress("UNCHECKED_CAST")
    val CATALOG_TYPE: ParameterMapping = {
        when (it.type.isSubtypeOf<CatalogType>()) {
            true -> {
                val clazz = (it.type.classifier as KClass<out CatalogType>).java

                { key ->
                    GenericArguments.catalogedElement(key, clazz)
                }
            }
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun commandSource(parameter: KParameter): ((Text) -> CommandElement)? {
        val type = parameter.type.classifier as? KClass<*> ?: return null

        return if (parameter.findAnnotation<Source>() != null && type.isSubclassOf(CommandSource::class)) {
            { key -> CommandSourceCommandElement(key, type as KClass<out CommandSource>) }
        } else {
            null
        }
    }

    val DEFAULT = listOf(
        ::commandSource,
        PLAYER,
        USER,
        STRING,
        BOOLEAN,
        INT,
        LONG,
        DOUBLE,
        BIGINTEGER,
        BIGDECIMAL,
        LOCATION,
        VECTOR3D,
        WORLD,
        DIMENSION,
        PLUGIN,
        TEXT,
        CATALOG_TYPE
    )
}