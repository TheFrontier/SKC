package frontier.skc

import frontier.skc.annotation.Command
import frontier.skc.annotation.Permission
import frontier.skc.util.displayName
import frontier.skc.value.AnnotatedValueParameter
import frontier.ske.commandManager
import frontier.ske.text.text
import org.spongepowered.api.command.CommandCallable
import org.spongepowered.api.command.CommandMessageFormatting
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
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

    override fun getSuggestions(
        source: CommandSource,
        arguments: String,
        targetPosition: Location<World>?
    ): MutableList<String> {
        TODO("not implemented")
    }

    override fun getShortDescription(source: CommandSource): Optional<Text> {
        TODO("not implemented")
    }

    override fun getHelp(source: CommandSource): Optional<Text> {
        TODO("not implemented")
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