package frontier.skc.annotation

import frontier.skc.transform.ExecutionContext
import frontier.skc.transform.ExecutionTransformer
import frontier.skc.transform.ParameterParseTransformer
import org.spongepowered.api.command.CommandPermissionException
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandArgs

@ExecutionTransformingAnnotation
@ParameterParseTransformingAnnotation
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER)
annotation class Permission(val value: String) {
    companion object : ExecutionTransformer<Permission>, ParameterParseTransformer<Permission> {
        override fun transformExecution(src: CommandSource, context: ExecutionContext, annotation: Permission, next: () -> CommandResult): CommandResult {
            if (!src.hasPermission(annotation.value)) {
                throw CommandPermissionException()
            }

            return next()
        }

        override fun transformParameterParse(src: CommandSource, args: CommandArgs, annotation: Permission, next: () -> Any?): Any? {
            if (!src.hasPermission(annotation.value)) {
                throw CommandPermissionException()
            }

            return next()
        }
    }
}
