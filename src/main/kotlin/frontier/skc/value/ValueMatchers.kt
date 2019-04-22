package frontier.skc.value

import frontier.skc.annotation.AllOf
import frontier.skc.annotation.OrSource
import frontier.skc.annotation.RemainingJoined
import frontier.skc.annotation.Source
import frontier.skc.match.AnnotationMatch.onAnnotation
import frontier.skc.match.AnnotationMatch.onEmpty
import frontier.skc.match.SKCMatcher
import frontier.skc.match.TypeMatch.onSubtype
import frontier.skc.match.TypeMatch.onType
import frontier.skc.match.and
import frontier.skc.match.or
import frontier.skc.util.playerOrThrow
import frontier.ske.java.lang.toPlayer
import frontier.ske.java.lang.toUser
import frontier.ske.java.lang.toWorld
import frontier.ske.java.lang.toWorldProperties
import frontier.ske.java.util.unwrap
import frontier.ske.plugin.toPlugin
import frontier.ske.pluginManager
import frontier.ske.server
import frontier.ske.service.user.userStorageService
import frontier.ske.text.not
import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.args.ArgumentParseException
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.entity.living.player.User
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.text.Text
import org.spongepowered.api.world.World
import org.spongepowered.api.world.storage.WorldProperties
import kotlin.reflect.KClass

fun SKCMatcher.list() {
    parseComplex(onType<List<*>>() and onAnnotation<AllOf>()) { type, _ ->
        val typeParam = type.arguments[0].type ?: return@parseComplex null
        val parser = this.findParser(typeParam, typeParam.annotations) ?: return@parseComplex null

        return@parseComplex IAnnotatedValueParser { src, args, modifiers ->
            val values = arrayListOf<Any?>()

            while (args.hasNext()) {
                values += parser(src, args, modifiers)
            }

            return@IAnnotatedValueParser values
        }
    }
    completeComplex(onType<List<*>>() and onAnnotation<AllOf>()) { type, _ ->
        val typeParam = type.arguments[0].type ?: return@completeComplex null
        val parser = this.findParser(typeParam, typeParam.annotations) ?: return@completeComplex null
        val completer = this.findCompleter(typeParam, typeParam.annotations) ?: return@completeComplex null

        return@completeComplex IAnnotatedValueCompleter { src, args, modifiers ->
            while (args.hasNext()) {
                val state = args.snapshot
                try {
                    parser(src, args, modifiers)
                } catch (e: ArgumentParseException) {
                    args.applySnapshot(state)
                    return@IAnnotatedValueCompleter completer(src, args, modifiers)
                }
            }
            return@IAnnotatedValueCompleter emptyList()
        }
    }
    usage(onType<List<*>>() and onAnnotation<AllOf>(), ValueUsages.VARIADIC)
}

fun SKCMatcher.sources() {
    complete(onAnnotation<Source>(), ValueCompleters.EMPTY)
    usage(onAnnotation<Source>(), ValueUsages.EMPTY)

    parseTyped(onAnnotation<Source>()) { src, _, _ ->
        src
    }
    parseTyped(onAnnotation<Source>()) { src, _, _ ->
        src.playerOrThrow { CommandException(!"You must be a player to use this command.") }
    }
    parseTyped<User>(onAnnotation<Source>()) { src, _, _ ->
        src.playerOrThrow { CommandException(!"You must be a user to use this command.") }
    }
}

fun SKCMatcher.player() {
    parseTyped(onEmpty()) { _, args, _ ->
        val name = args.next()
        name.toPlayer()
            ?: throw args.createError(!"Could not find any player named '$name'")
    }
    parseTyped(onAnnotation<OrSource>()) { src, args, _ ->
        if (args.hasNext()) {
            val state = args.snapshot
            val name = args.next()

            try {
                name.toPlayer() ?: throw args.createError(!"Could not find any player named '$name'")
            } catch (e: ArgumentParseException) {
                args.applySnapshot(state)
                src.playerOrThrow { args.createError(!"You must specify a player.") }
            }
        } else {
            src.playerOrThrow { args.createError(!"You must specify a player.") }
        }
    }

    complete(onType<Player>() and (onEmpty() or onAnnotation<OrSource>())) { _, _, _ ->
        server.onlinePlayers.map { it.name }
    }
    usage(onType<Player>() and (onEmpty() or onAnnotation<OrSource>()), ValueUsages.SINGLE)
}

fun SKCMatcher.user() {
    parseTyped(onEmpty()) { _, args, _ ->
        val name = args.next()
        name.toUser()
            ?: throw args.createError(!"Could not find any user named '$name'")
    }
    parseTyped<User>(onAnnotation<OrSource>()) { src, args, _ ->
        if (args.hasNext()) {
            val state = args.snapshot
            val name = args.next()

            try {
                name.toPlayer() ?: throw args.createError(!"Could not find any user named '$name'")
            } catch (e: ArgumentParseException) {
                args.applySnapshot(state)
                src.playerOrThrow { args.createError(!"You must specify a user.") }
            }
        } else {
            src.playerOrThrow { args.createError(!"You must specify a user.") }
        }
    }

    complete(onType<User>() and (onEmpty() or onAnnotation<OrSource>())) { _, _, _ ->
        userStorageService.all.take(100).mapNotNull { it.name.unwrap() }
    }
    usage(onType<User>() and (onEmpty() or onAnnotation<OrSource>()), ValueUsages.SINGLE)
}

fun SKCMatcher.world() {
    parseTyped(onEmpty()) { _, args, _ ->
        val name = args.next()
        name.toWorld()
            ?: throw args.createError(!"Could not find any world named '$name'")
    }
    complete(onType<World>() and onEmpty()) { _, _, _ ->
        server.worlds.map { it.name }
    }
    usage(onType<World>() and onEmpty(), ValueUsages.SINGLE)

    parseTyped(onEmpty()) { _, args, _ ->
        val name = args.next()
        name.toWorldProperties()
            ?: throw args.createError(!"Could not find any loaded/unloaded world named '$name'")
    }
    complete(onType<WorldProperties>() and onEmpty()) { _, _, _ ->
        server.allWorldProperties.map { it.worldName }
    }
    usage(onType<WorldProperties>() and onEmpty(), ValueUsages.SINGLE)
}

fun SKCMatcher.plugin() {
    parseTyped(onEmpty()) { _, args, _ ->
        val name = args.next()
        name.toPlugin()
            ?: throw args.createError(!"Could not find any plugin named '$name'")
    }
    complete(onType<PluginContainer>() and onEmpty()) { _, _, _ ->
        pluginManager.plugins.map { it.id }
    }
    usage(onType<PluginContainer>() and onEmpty(), ValueUsages.SINGLE)
}

fun SKCMatcher.string() {
    parseTyped<String>(onEmpty()) { _, args, _ ->
        args.next()
    }
    usage(onType<String>() and onEmpty(), ValueUsages.SINGLE)

    parseTyped(onAnnotation<RemainingJoined> { !it.raw }) { _, args, _ ->
        val builder = StringBuilder()
        while (args.hasNext()) {
            builder.append(args.next())
        }
        builder.toString()
    }

    complete(onType<String>(), ValueCompleters.EMPTY)
    usage(onType<String>() and onAnnotation<RemainingJoined>(), ValueUsages.VARIADIC)
}

fun SKCMatcher.bool() = choices(
    mapOf(
        "true" to true, "t" to true, "yes" to true, "y" to true, "1" to true,
        "false" to false, "f" to false, "no" to false, "n" to false, "0" to false
    )
)

fun SKCMatcher.byte() = number(String::toByte, String::toByte) { !"Expected an integer, but input '$it' was not." }

fun SKCMatcher.short() = number(String::toShort, String::toShort) { !"Expected an integer, but input '$it' was not." }

fun SKCMatcher.int() = number(String::toInt, String::toInt) { !"Expected an integer, but input '$it' was not." }

fun SKCMatcher.long() = number(String::toLong, String::toLong) { !"Expected an integer, but input '$it' was not." }

fun SKCMatcher.float() = number(String::toFloat) { !"Expected a number, but input '$it' was not." }

fun SKCMatcher.double() = number(String::toDouble) { !"Expected a number, but input '$it' was not." }

inline fun <reified T : Number> SKCMatcher.number(
    crossinline parseNum: (String) -> T,
    crossinline parseRadix: (String, Int) -> T,
    crossinline errorFunc: (String) -> Text
) {
    parseTyped(onEmpty()) { _, args, _ ->
        val input = args.next()
        try {
            when {
                input.startsWith("0x") -> parseRadix(input.substring(2), 16)
                input.startsWith("0b") -> parseRadix(input.substring(2), 2)
                else -> parseNum(input)
            }
        } catch (e: NumberFormatException) {
            throw args.createError(errorFunc(input))
        }
    }
    complete(onType<T>() and onEmpty(), ValueCompleters.EMPTY)
    usage(onType<T>() and onEmpty(), ValueUsages.SINGLE)
}

inline fun <reified T : Number> SKCMatcher.number(
    crossinline parseNum: (String) -> T,
    crossinline errorFunc: (String) -> Text
) {
    parseTyped(onEmpty()) { _, args, _ ->
        val input = args.next()
        try {
            parseNum(input)
        } catch (e: NumberFormatException) {
            throw args.createError(errorFunc(input))
        }
    }
    complete(onType<T>() and onEmpty(), ValueCompleters.EMPTY)
    usage(onType<T>() and onEmpty(), ValueUsages.SINGLE)
}

inline fun <reified T : Any> SKCMatcher.choices(map: Map<String, T>) {
    parseTyped(onEmpty()) { _, args, _ ->
        val choice = args.next()
        map[choice]
            ?: throw args.createError(!"Argument '$choice' is not a valid choice.\nValid choices: ${map.keys}")
    }
    complete(onType<T>() and onEmpty()) { _, _, _ ->
        map.keys.toList()
    }
    usage(onType<T>() and onEmpty(), ValueUsages.SINGLE)
}

fun SKCMatcher.enum() {
    parseComplex(onSubtype(Enum::class) and onEmpty()) { type, annotations ->
        val choices = (type.classifier as KClass<Enum<*>>).java.enumConstants.associateBy { it.name }

        IAnnotatedValueParser { _, args, _ ->
            val choice = args.next()
            choices[choice]
                ?: throw args.createError(!"Argument '$choice' is not a valid choice.\nValid choices: ${choices.keys.joinToString()}")
        }
    }
    completeComplex(onSubtype(Enum::class) and onEmpty()) { type, annotations ->
        IAnnotatedValueCompleter { _, _, _ ->
            (type.classifier as KClass<Enum<*>>).java.enumConstants.map { it.name }
        }
    }
    usage(onSubtype(Enum::class) and onEmpty(), ValueUsages.SINGLE)
}