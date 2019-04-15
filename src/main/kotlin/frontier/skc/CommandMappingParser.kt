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

fun KFunction<*>.newSpec(mappings: List<ParameterMapping>): CommandSpec.Builder {
    val spec = CommandSpec.builder()

    this.checkCommand()
    this.applyAnnotations(spec)

    spec.arguments(this.mapParameters(mappings))
    spec.executor(this.createExecutor())

    return spec
}

fun KFunction<*>.checkCommand() {
    require(this.returnType.isSubtypeOf<CommandResult>() || this.returnType.isSubtypeOf<Unit>()) {
        "Unsupported return type (${this.returnType}), must be CommandResult or Unit"
    }

    require(!this.isAbstract) { "Unsupported function modifier: abstract" }
    require(!this.isExternal) { "Unsupported function modifier: external" }
    require(!this.isInfix) { "Unsupported function modifier: infix" }
    require(!this.isInline) { "Unsupported function modifier: inline" }
    require(!this.isOperator) { "Unsupported function modifier: operator" }
    require(!this.isSuspend) { "Unsupported function modifier: suspend" }

    this.isAccessible = true
}

fun KFunction<*>.mapParameters(mappings: List<ParameterMapping>): CommandElement {
    val flags = GenericArguments.flags()
    val elements = LinkedList<CommandElement>()

    for (parameter in this.parameters) {
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
                flags.valueFlag(mappings.match(parameter), *flag.specs)
            }
        } else {
            // Non-flag element.
            elements += mappings.match(parameter)
        }
    }

    return flags.buildWith(GenericArguments.seq(*elements.toTypedArray()))
}

fun KFunction<*>.createExecutor(): CommandExecutor = CommandExecutor { _, ctx ->
    try {
        val result = this.callBy(this.parameters.buildCallingArguments(ctx))

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

fun KAnnotatedElement.applyAnnotations(spec: CommandSpec.Builder) {
    this.findAnnotation<Permission>()?.let { spec.permission(it.value) }
    this.findAnnotation<Description>()?.let { spec.description(+it.value) }
}

fun List<KParameter>.buildCallingArguments(ctx: CommandContext): Map<KParameter, Any?> {
    val values = hashMapOf<KParameter, Any?>()

    for (parameter in this) {
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

fun List<ParameterMapping>.match(parameter: KParameter): CommandElement {
    var element: CommandElement? = null

    for (mapper in this) {
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


class ObjectInstanceParameterMapping(private val clazz: KClass<*>, private val instance: Any) : ParameterMapping {
    override fun invoke(parameter: KParameter): ((Text) -> CommandElement)? = when (parameter.type.classifier) {
        clazz -> { key -> ConstantNoUsageCommandElement(key, instance) }
        else -> null
    }
}
