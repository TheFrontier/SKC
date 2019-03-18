package frontier.skc.util

import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandArgs
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.args.CommandElement
import org.spongepowered.api.text.Text

class ConstantNoUsageCommandElement(key: Text, private val value: Any) : CommandElement(key) {

    override fun parseValue(source: CommandSource, args: CommandArgs): Any? =
        value

    override fun complete(src: CommandSource, args: CommandArgs, context: CommandContext): List<String> =
        emptyList()

    override fun getUsage(src: CommandSource): Text =
        Text.EMPTY
}