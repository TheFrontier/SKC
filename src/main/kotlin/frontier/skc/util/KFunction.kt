package frontier.skc.util

import frontier.skc.ParameterMapping
import frontier.skc.annotation.Flag
import frontier.skc.annotation.Permission
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.args.CommandElement
import org.spongepowered.api.command.args.GenericArguments
import org.spongepowered.api.command.spec.CommandExecutor
import org.spongepowered.api.command.spec.CommandSpec
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible

fun KFunction<*>.newSpec(mappings: List<ParameterMapping>): CommandSpec.Builder {
    val spec = CommandSpec.builder()

    this.checkCommand()
    this.applyAnnotations(spec)

    spec.arguments(this.mapParameters(mappings))
    spec.executor(this.createExecutor())

    return spec
}

fun KFunction<*>.checkCommand() {
    require(this.isAccessible) { "Function must be accessible." }

    require(this.returnType.isSubtypeOf<CommandResult>() || this.returnType.isSubtypeOf<Unit>()) {
        "Unsupported return type (${this.returnType}), must be CommandResult or Unit"
    }

    require(!this.isAbstract) { "Unsupported function modifier: abstract" }
    require(!this.isExternal) { "Unsupported function modifier: external" }
    require(!this.isInfix) { "Unsupported function modifier: infix" }
    require(!this.isInline) { "Unsupported function modifier: inline" }
    require(!this.isOperator) { "Unsupported function modifier: operator" }
    require(!this.isSuspend) { "Unsupported function modifier: suspend" }
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
    val result = this.callBy(this.parameters.buildCallingArguments(ctx))

    if (result is CommandResult) {
        result
    } else {
        CommandResult.success()
    }
}