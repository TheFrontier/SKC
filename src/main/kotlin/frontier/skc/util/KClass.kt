package frontier.skc.util

import com.google.inject.Injector
import frontier.skc.ParameterMapping
import frontier.skc.annotation.Command
import frontier.skc.annotation.Executor
import org.spongepowered.api.command.spec.CommandSpec
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions

fun KClass<*>.newSpec(mappings: List<ParameterMapping>, injector: Injector? = null): CommandSpec.Builder {
    val spec = CommandSpec.builder()

    val instance = this.objectInstance ?: requireNotNull(injector?.getInstance(this.java)) {
        "Could not instantiate Command for ${this.simpleName}: must be an object, or provide an injector if class"
    }

    val finalMappings = mappings + ObjectInstanceParameterMapping(this, instance)

    this.checkCommand()
    this.applyAnnotations(spec)

    // Register child objects/classes.
    for (childClass in this.nestedClasses) {
        val aliases = childClass.findAnnotation<Command>()?.aliases ?: continue
        val childSpec = childClass.newSpec(finalMappings).build()
        spec.child(childSpec, *aliases)
    }

    var hasDefault = false

    // Register child functions and the default executor, if available.
    for (childFunction in this.functions) {
        if (childFunction.findAnnotation<Executor>() != null) {
            // Found a default executor
            require(!hasDefault) { "${this.simpleName} already has a default executor." }

            spec.arguments(childFunction.mapParameters(finalMappings))
            spec.executor(childFunction.createExecutor())

            hasDefault = true
        }

        val aliases = childFunction.findAnnotation<Command>()?.aliases ?: continue
        val childSpec = childFunction.newSpec(finalMappings).build()
        spec.child(childSpec, *aliases)
    }

    return spec
}

fun KClass<*>.checkCommand() {
    require(!this.isAbstract) { "Unsupported class modifier: abstract" }
    require(!this.isSealed) { "Unsupported class modifier: sealed" }
}