package frontier.skc.transform

import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.CommandSource

typealias ExecutionContext = MutableMap<String, Any?>

interface ExecutionTransformer {

    @Throws(CommandException::class)
    fun transform(src: CommandSource, context: ExecutionContext, next: () -> Unit)
}