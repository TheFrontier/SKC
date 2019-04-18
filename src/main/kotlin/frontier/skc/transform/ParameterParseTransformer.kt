package frontier.skc.transform

import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandArgs

interface ParameterParseTransformer<T : Annotation> {
    fun transformParameterParse(src: CommandSource, args: CommandArgs, annotation: T, next: () -> Any?): Any?
}