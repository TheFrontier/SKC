package frontier.skc.transform

import org.spongepowered.api.command.CommandPermissionException
import org.spongepowered.api.command.CommandSource

class PermissionTransformer(private val permission: String) : ExecutionTransformer {

    override fun transform(src: CommandSource, context: ExecutionContext, next: () -> Unit) {
        if (!src.hasPermission(permission)) {
            throw CommandPermissionException()
        }

        next()
    }
}