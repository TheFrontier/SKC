package frontier.skc

import org.spongepowered.api.command.args.CommandElement
import org.spongepowered.api.text.Text
import kotlin.reflect.KParameter

typealias ParameterMapping = (KParameter) -> ((Text) -> CommandElement)?