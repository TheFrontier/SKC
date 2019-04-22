package frontier.skc.util

import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.entity.living.player.Player
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@UseExperimental(ExperimentalContracts::class)
inline fun CommandSource.playerOrThrow(err: () -> Exception): Player {
    contract {
        returns() implies (this@playerOrThrow is Player)
    }

    if (this !is Player) {
        throw err()
    }

    return this
}