package frontier.skc.value

import frontier.skc.annotation.RemainingJoined
import frontier.skc.annotation.Source
import frontier.skc.match.AnnotationMatch.onAnnotation
import frontier.skc.match.AnnotationMatch.onEmpty
import frontier.skc.match.SKCMatcher
import frontier.skc.match.TypeMatch.onType
import frontier.skc.match.and
import frontier.ske.java.lang.toPlayer
import frontier.ske.server
import frontier.ske.text.not
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.entity.living.player.Player

fun SKCMatcher.commandSource() {
    parse(onType<CommandSource>() and onAnnotation<Source>()) { src, _, _ ->
        src
    }
    complete(onType<CommandSource>() and onAnnotation<Source>(), ValueCompleters.EMPTY)
    usage(onType<CommandSource>() and onAnnotation<Source>(), ValueUsages.EMPTY)
}

fun SKCMatcher.player() {
    parse(onType<Player>() and onEmpty()) { _, args, _ ->
        val name = args.next()
        name.toPlayer()
            ?: throw args.createError(!"Could not find any player named '$name'")
    }
    complete(onType<Player>() and onEmpty()) { _, _, _ ->
        server.onlinePlayers.map { it.name }
    }
    usage(onType<Player>() and onEmpty(), ValueUsages.SINGLE)
}

fun SKCMatcher.string() {
    parse(onType<String>() and onEmpty()) { _, args, _ ->
        args.next()
    }
    usage(onType<String>() and onEmpty(), ValueUsages.SINGLE)

    parse(onType<String>() and onAnnotation<RemainingJoined> { !it.raw }) { _, args, _ ->
        val builder = StringBuilder()
        while (args.hasNext()) {
            builder.append(args.next())
        }
        builder.toString()
    }

    complete(onType<String>(), ValueCompleters.EMPTY)
    usage(onType<String>() and onAnnotation<RemainingJoined>(), ValueUsages.VARIADIC)
}