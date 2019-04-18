package frontier.skc

import frontier.skc.annotation.Command
import frontier.skc.annotation.Description
import frontier.skc.annotation.Permission
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
import kotlin.reflect.full.findAnnotation

class KFunctionCallable(
    private val function: KFunction<*>,
    private val parameters: List<Pair<KParameter, AnnotatedValueParameter>>
) : CommandCallable {

    private val aliases = function.findAnnotation<Command>()?.aliases?.toList().orEmpty()

    private val permission: String? = function.findAnnotation<Permission>()?.value
    private val description: Optional<Text> = function.findAnnotation<Description>()?.value?.unaryPlus().wrap()

    private val tokenizer: InputTokenizer = InputTokenizer.quotedStrings(false)

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

        val result = function.callBy(callMap)

        return if (result is CommandResult) {
            result
        } else {
            CommandResult.success()
        }
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
}