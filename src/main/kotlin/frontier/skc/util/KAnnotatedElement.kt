package frontier.skc.util

import frontier.skc.annotation.Description
import frontier.skc.annotation.Permission
import frontier.ske.text.unaryPlus
import org.spongepowered.api.command.spec.CommandSpec
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.full.findAnnotation

inline fun <reified T : Annotation> List<Annotation>.findAnnotation(): T? =
    this.firstOrNull { it is T } as T?

fun KAnnotatedElement.applyAnnotations(spec: CommandSpec.Builder) {
    this.findAnnotation<Permission>()?.let { spec.permission(it.value) }
    this.findAnnotation<Description>()?.let { spec.description(+it.value) }
}