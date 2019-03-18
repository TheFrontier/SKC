package frontier.skc.util

import frontier.skc.ParameterMapping
import org.spongepowered.api.command.args.CommandElement
import org.spongepowered.api.text.Text
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

class ObjectInstanceParameterMapping(private val clazz: KClass<*>, private val instance: Any) : ParameterMapping {
    override fun invoke(parameter: KParameter): ((Text) -> CommandElement)? = when (parameter.type.classifier) {
        clazz -> { key -> ConstantNoUsageCommandElement(key, instance) }
        else -> null
    }
}