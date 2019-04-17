package frontier.skc.value

import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandArgs
import org.spongepowered.api.text.Text

typealias AnnotatedValueParser = (src: CommandSource, args: CommandArgs, modifiers: List<Annotation>) -> Any?

typealias AnnotatedValueCompleter = (src: CommandSource, args: CommandArgs, modifiers: List<Annotation>) -> List<String>

typealias AnnotatedValueUsage = (src: CommandSource, key: Text) -> Text

data class AnnotatedValueParameter(
    val parser: AnnotatedValueParser,
    val completer: AnnotatedValueCompleter,
    val usage: AnnotatedValueUsage
)