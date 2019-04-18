package frontier.skc.annotation

import frontier.skc.transform.ExecutionContext
import frontier.skc.transform.ExecutionTransformer
import org.spongepowered.api.command.CommandPermissionException
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER)
annotation class Permission(val value: String) {
    companion object : ExecutionTransformer<Permission> {
        override fun transform(src: CommandSource, context: ExecutionContext, annotation: Permission, next: () -> CommandResult): CommandResult {
            if (!src.hasPermission(annotation.value)) {
                throw CommandPermissionException()
            }

            return next()
        }
    }
}
