package frontier.skc

import com.google.inject.Injector
import frontier.skc.annotation.Command
import frontier.ske.commandManager
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation

class SKCCommand(
    private val mappings: List<ParameterMapping> = ParameterMappings.DEFAULT,
    private val injector: Injector? = null
) {

    inline fun <reified T : Any> register(plugin: Any) = register(plugin, T::class)

    fun register(plugin: Any, clazz: KClass<*>) {
        val command = requireNotNull(clazz.findAnnotation<Command>()) {
            "${clazz.simpleName} must be annotated with @Command"
        }

        val spec = clazz.newSpec(mappings, injector).build()

        commandManager.register(plugin, spec, *command.aliases)
    }

    fun register(plugin: Any, function: KFunction<*>) {
        val command = requireNotNull(function.findAnnotation<Command>()) {
            "${function.name} must be annotated with @Command"
        }

        val spec = function.newSpec(mappings).build()

        commandManager.register(plugin, spec, *command.aliases)
    }
}