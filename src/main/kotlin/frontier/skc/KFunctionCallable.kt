package frontier.skc

import frontier.skc.annotation.Command
import frontier.skc.annotation.Description
import frontier.skc.annotation.ExecutionTransformerAnnotation
import frontier.skc.annotation.Permission
import frontier.skc.transform.ExecutionContext
import frontier.skc.transform.ExecutionTransformer
import frontier.skc.util.displayName
import frontier.skc.value.AnnotatedValueParameter
import frontier.ske.commandManager
import frontier.ske.java.util.wrap
import frontier.ske.text.text
import frontier.ske.text.unaryPlus
import org.spongepowered.api.command.*
import org.spongepowered.api.command.args.ArgumentParseException
import org.spongepowered.api.command.args.CommandArgs
import org.spongepowered.api.command.args.parsing.InputTokenizer
import org.spongepowered.api.command.dispatcher.SimpleDispatcher
import org.spongepowered.api.text.Text
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.World
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.findAnnotation

class KFunctionCallable(
    function: KFunction<*>,
    private val parameters: List<Pair<KParameter, AnnotatedValueParameter>>
) : CommandCallable {

    private val aliases = function.findAnnotation<Command>()?.aliases?.toList().orEmpty()

    private val permission: String? = function.findAnnotation<Permission>()?.value
    private val description: Optional<Text> = function.findAnnotation<Description>()?.value?.unaryPlus().wrap()

    private val tokenizer: InputTokenizer = InputTokenizer.quotedStrings(false)

    private val executor: Executor = function.annotations
        .filter { it::class.findAnnotation<ExecutionTransformerAnnotation>() != null }
        .foldRight<Annotation, Executor>(Executor.Function(function)) { annot, exec ->
            (annot::class.companionObjectInstance as? ExecutionTransformer<*>)
                ?.let { Executor.Transformer(annot, it, exec) } ?: exec }


    override fun testPermission(source: CommandSource): Boolean {
        return permission == null || source.hasPermission(permission)
    }

    override fun process(source: CommandSource, arguments: String): CommandResult {
        val args = CommandArgs(arguments, tokenizer.tokenize(arguments, false))

        val callMap = hashMapOf<KParameter, Any?>()

        for ((param, value) in parameters) {
            val state = args.snapshot
            try {
                val parsed = value.parser(source, args, param.annotations)
                callMap[param] = parsed
            } catch (e: ArgumentParseException) {
                when {
                    param.isOptional -> args.applySnapshot(state)
                    param.type.isMarkedNullable -> {
                        args.applySnapshot(state)
                        callMap[param] = null
                    }
                    else -> throw e
                }
            }
        }

        return executor.run(source, callMap)
    }

    override fun getSuggestions(source: CommandSource, arguments: String, targetPos: Location<World>?): List<String> {
        if (!testPermission(source)) {
            throw CommandPermissionException()
        }

        val args = CommandArgs(arguments, tokenizer.tokenize(arguments, true))
        val completions = hashSetOf<String>()

        for ((param, value) in parameters) {
            val state = args.snapshot

            try {
                value.parser(source, args, param.annotations)

                if (state == args.snapshot) {
                    completions += value.completer(source, args, param.annotations)
                    args.applySnapshot(state)
                } else if (args.hasNext()) {
                    completions.clear()
                } else {
                    args.applySnapshot(state)
                    completions += value.completer(source, args, param.annotations)

                    if (!param.isOptional) {
                        break
                    }

                    args.applySnapshot(state)
                }
            } catch (ignored: ArgumentParseException) {
                args.applySnapshot(state)
                completions += value.completer(source, args, param.annotations)
                break
            }
        }

        return completions.toList()
    }

    override fun getShortDescription(source: CommandSource): Optional<Text> {
        return description
    }

    override fun getHelp(source: CommandSource): Optional<Text> {
        return description
    }

    override fun getUsage(source: CommandSource): Text {
        val result = Text.builder()

        val iterator = parameters.iterator()
        while (iterator.hasNext()) {
            val (parameter, value) = iterator.next()
            val usage = value.usage(source, parameter.displayName.text())
            if (!usage.isEmpty) {
                result.append(usage)
                if (iterator.hasNext()) {
                    result.append(CommandMessageFormatting.SPACE_TEXT)
                }
            }
        }

        return result.build()
    }

    fun register(dispatcher: SimpleDispatcher) {
        dispatcher.register(this, aliases)
    }

    fun register(plugin: Any) {
        commandManager.register(plugin, this, aliases)
    }

    private sealed class Executor {
        abstract fun run(source: CommandSource, context: ExecutionContext): CommandResult
        class Function(val function: KFunction<*>): Executor() {
            override fun run(source: CommandSource, context: ExecutionContext): CommandResult {
                return function.callBy(context) as? CommandResult ?: CommandResult.success()
            }
        }
        class Transformer(val annotation: Annotation, val transformer: ExecutionTransformer<*>, val next: Executor)
            : Executor() {

            override fun run(source: CommandSource, context: ExecutionContext): CommandResult {
                @Suppress("UNCHECKED_CAST")
                return (transformer as ExecutionTransformer<Annotation>).transform(source, context, annotation) {
                    next.run(source, context)
                }
            }
        }
    }
}
