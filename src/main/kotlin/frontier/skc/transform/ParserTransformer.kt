package frontier.skc.transform

import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandArgs

interface ParserTransformer<T : Annotation> {
    fun transformParser(src: CommandSource, args: CommandArgs, annotation: T, next: () -> Any?): Any?
}