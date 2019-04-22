package frontier.skc.value

import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandArgs
import org.spongepowered.api.text.Text

interface IAnnotatedValueParser : AnnotatedValueParser {

    override fun invoke(src: CommandSource, args: CommandArgs, modifiers: List<Annotation>): Any?

    companion object {

        inline operator fun invoke(crossinline parser: AnnotatedValueParser): IAnnotatedValueParser =
            object : IAnnotatedValueParser {
                override fun invoke(src: CommandSource, args: CommandArgs, modifiers: List<Annotation>): Any? =
                    parser(src, args, modifiers)
            }
    }
}

interface IAnnotatedValueCompleter : AnnotatedValueCompleter {

    override fun invoke(src: CommandSource, args: CommandArgs, modifiers: List<Annotation>): List<String>

    companion object {

        inline operator fun invoke(crossinline completer: AnnotatedValueCompleter): IAnnotatedValueCompleter =
            object : IAnnotatedValueCompleter {
                override fun invoke(src: CommandSource, args: CommandArgs, modifiers: List<Annotation>): List<String> =
                    completer(src, args, modifiers)
            }
    }
}

interface IAnnotatedValueUsage : AnnotatedValueUsage {

    override fun invoke(src: CommandSource, key: Text): Text

    companion object {

        inline operator fun invoke(crossinline usage: AnnotatedValueUsage): IAnnotatedValueUsage =
            object : IAnnotatedValueUsage {
                override fun invoke(src: CommandSource, key: Text): Text =
                    usage(src, key)
            }
    }
}