package frontier.skc

import com.google.inject.Injector
import frontier.skc.annotation.Command
import frontier.skc.annotation.Description
import frontier.skc.annotation.Executor
import frontier.skc.annotation.Flag
import frontier.skc.annotation.Permission
import frontier.skc.annotation.Weak
import frontier.skc.util.ConstantNoUsageCommandElement
import frontier.skc.util.effectiveName
import frontier.skc.util.isSubtypeOf
import frontier.skc.util.isType
import frontier.ske.commandManager
import frontier.ske.text.not
import frontier.ske.text.unaryPlus
import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.args.CommandElement
import org.spongepowered.api.command.args.GenericArguments
import org.spongepowered.api.command.spec.CommandExecutor
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.text.Text
import java.lang.reflect.InvocationTargetException
import java.util.LinkedList
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.isAccessible

class SKCCommand(
    private val mappings: List<ParameterMapping> = ParameterMappings.DEFAULT,
    private val injector: Injector? = null
) {

    inline fun <reified T : Any> register(plugin: Any) = register(plugin, T::class)

    fun register(plugin: Any, clazz: KClass<*>) {
        val command = requireNotNull(clazz.findAnnotation<Command>()) {
            "${clazz.simpleName} must be annotated with @Command"
        }

        val spec = newSpec(clazz, mappings, injector).build()

        commandManager.register(plugin, spec, *command.aliases)
    }

    fun register(plugin: Any, function: KFunction<*>) {
        val command = requireNotNull(function.findAnnotation<Command>()) {
            "${function.name} must be annotated with @Command"
        }

        val spec = newSpec(function, mappings).build()

        commandManager.register(plugin, spec, *command.aliases)
    }

    fun newSpec(clazz: KClass<*>, mappings: List<ParameterMapping>, injector: Injector? = null): CommandSpec.Builder {
        val spec = CommandSpec.builder()

        val instance = clazz.objectInstance ?: requireNotNull(injector?.getInstance(clazz.java)) {
            "Could not instantiate Command for ${clazz.simpleName}: must be an object, or provide an injector if class"
        }

        val finalMappings = mappings + ObjectInstanceParameterMapping(clazz, instance)

        checkCommand(clazz)
        applyAnnotations(clazz, spec)

        // Register child objects/classes.
        for (childClass in clazz.nestedClasses) {
            val aliases = childClass.findAnnotation<Command>()?.aliases ?: continue
            val childSpec = newSpec(childClass, finalMappings).build()
            spec.child(childSpec, *aliases)
        }

        var hasDefault = false

        // Register child functions and the default executor, if available.
        for (childFunction in clazz.functions) {
            if (childFunction.findAnnotation<Executor>() != null) {
                // Found a default executor
                require(!hasDefault) { "${clazz.simpleName} already has a default executor." }

                spec.arguments(mapParameters(childFunction, finalMappings))
                spec.executor(createExecutor(childFunction))

                hasDefault = true
            }

            val aliases = childFunction.findAnnotation<Command>()?.aliases ?: continue
            val childSpec = newSpec(childFunction, finalMappings).build()
            spec.child(childSpec, *aliases)
        }

        return spec
    }

    fun checkCommand(clazz: KClass<*>) {
        require(!clazz.isAbstract) { "Unsupported class modifier: abstract" }
        require(!clazz.isSealed) { "Unsupported class modifier: sealed" }
    }

    fun newSpec(function: KFunction<*>, mappings: List<ParameterMapping>): CommandSpec.Builder {
        val spec = CommandSpec.builder()

        checkCommand(function)
        applyAnnotations(function, spec)

        spec.arguments(mapParameters(function, mappings))
        spec.executor(createExecutor(function))

        return spec
    }

    fun checkCommand(function: KFunction<*>) {
        require(function.returnType.isSubtypeOf<CommandResult>() || function.returnType.isSubtypeOf<Unit>()) {
            "Unsupported return type (${function.returnType}), must be CommandResult or Unit"
        }

        require(!function.isAbstract) { "Unsupported function modifier: abstract" }
        require(!function.isExternal) { "Unsupported function modifier: external" }
        require(!function.isInfix) { "Unsupported function modifier: infix" }
        require(!function.isInline) { "Unsupported function modifier: inline" }
        require(!function.isOperator) { "Unsupported function modifier: operator" }
        require(!function.isSuspend) { "Unsupported function modifier: suspend" }

        function.isAccessible = true
    }

    fun mapParameters(function: KFunction<*>, mappings: List<ParameterMapping>): CommandElement {
        val flags = GenericArguments.flags()
        val elements = LinkedList<CommandElement>()

        for (parameter in function.parameters) {
            val flag = parameter.findAnnotation<Flag>()

            if (flag != null) {
                // Flag element.
                if (parameter.type.isType<Boolean>()) {
                    // Boolean flag.
                    val permission = parameter.findAnnotation<Permission>()

                    when (permission) {
                        null -> flags.flag(*flag.specs)
                        else -> flags.permissionFlag(permission.value, *flag.specs)
                    }
                } else {
                    // Value flag.
                    flags.valueFlag(match(mappings, parameter), *flag.specs)
                }
            } else {
                // Non-flag element.
                elements += match(mappings, parameter)
            }
        }

        return flags.buildWith(GenericArguments.seq(*elements.toTypedArray()))
    }

    fun createExecutor(function: KFunction<*>): CommandExecutor = CommandExecutor { _, ctx ->
        try {
            val result = function.callBy(buildCallingArguments(function.parameters, ctx))

            if (result is CommandResult) {
                result
            } else {
                CommandResult.success()
            }
        } catch (e: InvocationTargetException) {
            val cause = e.cause

            when (cause) {
                is CommandException -> throw cause
                null -> {
                    e.printStackTrace()
                    throw CommandException(!"An error occurred while executing that command.", e)
                }
                else -> {
                    e.printStackTrace()
                    throw CommandException(!"An error occurred while executing that command.", cause)
                }
            }
        }
    }

    fun applyAnnotations(element: KAnnotatedElement, spec: CommandSpec.Builder) {
        element.findAnnotation<Permission>()?.let { spec.permission(it.value) }
        element.findAnnotation<Description>()?.let { spec.description(+it.value) }
    }

    fun buildCallingArguments(parameters: List<KParameter>, ctx: CommandContext): Map<KParameter, Any?> {
        val values = hashMapOf<KParameter, Any?>()

        for (parameter in parameters) {
            if (ctx.hasAny(parameter.effectiveName)) {
                // A value is available, use it.
                values[parameter] = ctx.requireOne(parameter.effectiveName)
            } else if (parameter.type.isMarkedNullable) {
                // No value is available, but the type is nullable, so set it to null.
                values[parameter] = null
            } else if (!parameter.isOptional) {
                // There is no available value to use! What do we do?!
                throw CommandException(!"No value found for parameter '${parameter.effectiveName}'")
            }
        }

        return values
    }

    fun match(mappings: List<ParameterMapping>, parameter: KParameter): CommandElement {
        var element: CommandElement? = null

        for (mapper in mappings) {
            element = mapper(parameter)?.invoke(!parameter.effectiveName)

            if (element != null) break
        }

        var result = requireNotNull(element) {
            "Could not find a ParameterMapping that matches ${parameter.type}"
        }

        parameter.findAnnotation<Permission>()?.let {
            result = GenericArguments.requiringPermission(result, it.value)
        }

        if (parameter.isOptional || parameter.type.isMarkedNullable) {
            result = when {
                parameter.findAnnotation<Weak>() != null -> GenericArguments.optionalWeak(result)
                else -> GenericArguments.optional(result)
            }
        }

        return result
    }
}