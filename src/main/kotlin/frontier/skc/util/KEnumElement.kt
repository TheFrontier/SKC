package frontier.skc.util

import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.PatternMatchingCommandElement
import org.spongepowered.api.text.Text
import kotlin.reflect.KClass

class KEnumElement(key: Text, private val type: KClass<out Enum<*>>) : PatternMatchingCommandElement(key) {

    private val values: Map<String, Enum<*>> =
        type.java.enumConstants.map { it.name.toLowerCase() to it }.toMap()

    override fun getChoices(source: CommandSource): Iterable<String> =
        this.values.keys

    override fun getValue(choice: String): Any =
        this.values[choice.toLowerCase()]
            ?: throw IllegalArgumentException("No enum constant ${this.type.simpleName}.$choice")
}