package frontier.skc.util

import frontier.ske.text.not
import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.args.CommandContext
import kotlin.reflect.KParameter

val KParameter.effectiveName: String get() = this.name ?: "arg${this.index}"

fun List<KParameter>.buildCallingArguments(ctx: CommandContext): Map<KParameter, Any?> {
    val values = hashMapOf<KParameter, Any?>()

    for (parameter in this) {
        if (ctx.hasAny(parameter.effectiveName)) {
            // A value is available, use it.
            values[parameter] = ctx.requireOne(parameter.effectiveName)
        } else if (parameter.type.isMarkedNullable) {
            // No value is available, but the type is nullable, so set it to null.
            values[parameter] = null
        } else if (!parameter.isOptional) {
            // There is no available value to use! What do we do?!
            throw CommandException(!"No value found for parameter '${parameter.effectiveName}'")
        }
    }

    return values
}