package frontier.skc.util

import frontier.skc.ParameterMapping
import frontier.skc.annotation.Permission
import frontier.skc.annotation.Weak
import frontier.ske.text.not
import org.spongepowered.api.command.args.CommandElement
import org.spongepowered.api.command.args.GenericArguments
import org.spongepowered.api.text.Text
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation

inline fun <reified T> matchOnType(crossinline process: ParameterMapping): ParameterMapping =
    { parameter ->
        when (parameter.type.classifier) {
            T::class -> process(parameter)
            else -> null
        }
    }

inline fun <reified T> directlyMatchOnType(noinline init: (Text) -> CommandElement): ParameterMapping =
    { parameter ->
        when (parameter.type.classifier) {
            T::class -> init
            else -> null
        }
    }