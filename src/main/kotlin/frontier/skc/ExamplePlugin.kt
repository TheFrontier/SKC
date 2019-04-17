package frontier.skc

import frontier.skc.annotation.Command
import frontier.skc.annotation.RemainingJoined
import frontier.skc.annotation.Source
import frontier.skc.match.SKCMatcher
import frontier.skc.value.commandSource
import frontier.skc.value.player
import frontier.skc.value.string
import frontier.ske.commandManager
import frontier.ske.text.not
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.plugin.Plugin

@Plugin(id = "skc-example")
class ExamplePlugin {

    @Listener
    fun onInit(event: GameInitializationEvent) {
        val matcher = SKCMatcher().apply {
            this.commandSource()
            this.player()
            this.string()
        }

        val test = KFunctionCallable(CommandExample::test, matcher.resolve(CommandExample::test))

        commandManager.register(this, test, "test")
    }
}

@Command("example")
object CommandExample {

    @Command("test")
    fun test(@Source src: CommandSource, player: Player, @RemainingJoined message: String) {
        player.sendMessage(!"${src.name} says: $message")
    }
}