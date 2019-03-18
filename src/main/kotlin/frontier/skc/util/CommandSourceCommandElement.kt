package frontier.skc.util

import frontier.ske.text.not
import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandArgs
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.args.CommandElement
import org.spongepowered.api.text.Text
import kotlin.reflect.KClass

class CommandSourceCommandElement(key: Text, private val required: KClass<out CommandSource> = CommandSource::class) :
    CommandElement(key) {

    override fun parseValue(source: CommandSource, args: CommandArgs): Any? {
        if (!required.isInstance(source)) {
            throw CommandException(!"You must be a ${required.simpleName} to use that command!")
        }

        return source
    }

    override fun complete(src: CommandSource, args: CommandArgs, context: CommandContext): List<String> =
        emptyList()

    override fun getUsage(src: CommandSource): Text =
        Text.EMPTY
}