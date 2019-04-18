package frontier.skc.transform

import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import kotlin.reflect.KParameter

typealias ExecutionContext = MutableMap<KParameter, Any?>

interface ExecutionTransformer<T : Annotation> {

    @Throws(CommandException::class)
    fun transform(src: CommandSource, context: ExecutionContext, annotation: T, next: () -> CommandResult): CommandResult
}