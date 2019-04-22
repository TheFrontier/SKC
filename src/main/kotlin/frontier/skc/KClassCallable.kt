package frontier.skc

import frontier.skc.annotation.Command
import frontier.skc.annotation.Description
import frontier.skc.annotation.Executor
import frontier.skc.annotation.Permission
import frontier.skc.match.SKCMatcher
import frontier.skc.match.TypeMatch
import frontier.skc.util.annotatedClasses
import frontier.skc.util.annotatedFunctions
import frontier.skc.value.ValueCompleters
import frontier.skc.value.ValueUsages
import frontier.ske.commandManager
import frontier.ske.java.util.wrap
import frontier.ske.text.not
import frontier.ske.text.unaryPlus
import org.spongepowered.api.command.*
import org.spongepowered.api.command.dispatcher.SimpleDispatcher
import org.spongepowered.api.text.Text
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.World
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class KClassCallable(clazz: KClass<*>, private val matcher: SKCMatcher) : CommandCallable {

    companion object {
        inline operator fun <reified T : Any> invoke(matcher: SKCMatcher) = KClassCallable(T::class, matcher)
    }

    init {
        require(clazz.objectInstance != null) { "Classes are not currently supported. Use objects." }

        matcher.parse(TypeMatch.onType(clazz)) { _, _, _ ->
            clazz.objectInstance
        }
        matcher.complete(TypeMatch.onType(clazz), ValueCompleters.EMPTY)
        matcher.usage(TypeMatch.onType(clazz), ValueUsages.EMPTY)
    }

    private val defaultExecutor: KFunctionCallable?

    init {
        val executors = clazz.annotatedFunctions<Executor>()

        if (executors.size > 1) {
            throw IllegalArgumentException("${clazz.simpleName} cannot have more than one function annotated with @Executor")
        }

        defaultExecutor = executors.firstOrNull()?.let { KFunctionCallable(it, matcher.resolve(it)) }
    }

    private val dispatcher = SimpleDispatcher(SimpleDispatcher.FIRST_DISAMBIGUATOR)

    init {
        for (child in clazz.annotatedFunctions<Command>()) {
            val callable = KFunctionCallable(child, matcher.resolve(child))
            callable.register(dispatcher)
        }

        for (child in clazz.annotatedClasses<Command>()) {
            val callable = KClassCallable(child, matcher)
            callable.register(dispatcher)
        }
    }

    private val aliases: List<String> = clazz.findAnnotation<Command>()?.aliases?.toList().orEmpty()
    private val permission: String? = clazz.findAnnotation<Permission>()?.value
    private val description: Optional<Text> = clazz.findAnnotation<Description>()?.value?.unaryPlus().wrap()

    override fun testPermission(src: CommandSource): Boolean {
        return permission == null || src.hasPermission(permission)
    }

    override fun process(src: CommandSource, arguments: String): CommandResult {
        if (!testPermission(src)) {
            throw CommandPermissionException()
        }

        return try {
            dispatcher.process(src, arguments)
        } catch (e: CommandNotFoundException) {
            if (defaultExecutor == null) {
                throw CommandException(!"Not a valid subcommand: ${e.command}", true)
            }
            defaultExecutor.process(src, arguments)
        } catch (e: CommandException) {
            if (defaultExecutor == null) {
                val message = e.message ?: throw e
                if (message.startsWith("No such child command: ")) {
                    val command = message.substring(23)
                    throw CommandException(!"Not a valid subcommand: $command", true)
                }
                throw e
            }
            defaultExecutor.process(src, arguments)
        }
    }

    override fun getSuggestions(src: CommandSource, arguments: String, target: Location<World>?): List<String> {
        val completions = hashSetOf<String>()

        if (defaultExecutor != null) {
            completions += defaultExecutor.getSuggestions(src, arguments, target)
        }

        completions += dispatcher.getSuggestions(src, arguments, target)

        return completions.toList()
    }

    override fun getShortDescription(src: CommandSource): Optional<Text> {
        return description
    }

    override fun getHelp(src: CommandSource): Optional<Text> {
        return description
    }

    override fun getUsage(src: CommandSource): Text {
        val usage = dispatcher.getUsage(src)

        if (defaultExecutor == null) return usage

        val defaultUsage = defaultExecutor.getUsage(src)

        if (defaultUsage.isEmpty) return usage

        return Text.of(usage, CommandMessageFormatting.PIPE_TEXT, defaultUsage)
    }

    fun register(dispatcher: SimpleDispatcher) {
        dispatcher.register(this, aliases)
    }

    fun register(plugin: Any) {
        commandManager.register(plugin, this, aliases)
    }
}