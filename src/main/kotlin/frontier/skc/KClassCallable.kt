package frontier.skc

import frontier.skc.annotation.Description
import frontier.skc.annotation.Executor
import frontier.skc.annotation.Permission
import frontier.skc.match.SKCMatcher
import frontier.skc.util.annotatedFunctions
import frontier.ske.java.util.wrap
import frontier.ske.text.unaryPlus
import org.spongepowered.api.command.CommandCallable
import org.spongepowered.api.command.CommandNotFoundException
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.parsing.InputTokenizer
import org.spongepowered.api.command.dispatcher.SimpleDispatcher
import org.spongepowered.api.text.Text
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.World
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class KClassCallable(private val clazz: KClass<*>, private val matcher: SKCMatcher) : CommandCallable {

    val defaultExecutor: KFunctionCallable?

    init {
        val executors = clazz.annotatedFunctions<Executor>()

        if (executors.size > 1) {
            throw IllegalArgumentException("${clazz.simpleName} cannot have more than one function annotated with @Executor")
        }

        defaultExecutor = executors.firstOrNull()?.let { KFunctionCallable(it, matcher.resolve(it)) }
    }

    private val dispatcher = SimpleDispatcher(SimpleDispatcher.FIRST_DISAMBIGUATOR)
    private val tokenizer: InputTokenizer = InputTokenizer.quotedStrings(false)

    private val permission: String? = clazz.findAnnotation<Permission>()?.value
    private val description: Optional<Text> = clazz.findAnnotation<Description>()?.value?.unaryPlus().wrap()

    override fun testPermission(src: CommandSource): Boolean {
        return permission == null || src.hasPermission(permission)
    }

    override fun process(src: CommandSource, arguments: String): CommandResult {
        return try {
            dispatcher.process(src, arguments)
        } catch (e: CommandNotFoundException) {
            if (defaultExecutor == null) {
                throw e
            }
            defaultExecutor.process(src, arguments)
        }
    }

    override fun getSuggestions(src: CommandSource, arguments: String, target: Location<World>?): List<String> {
        return emptyList()
    }

    override fun getShortDescription(src: CommandSource): Optional<Text> {
        return description
    }

    override fun getHelp(src: CommandSource): Optional<Text> {
        return description
    }

    override fun getUsage(src: CommandSource): Text {
        return defaultExecutor?.getUsage(src) ?: dispatcher.getUsage(src) ?: Text.EMPTY
    }
}