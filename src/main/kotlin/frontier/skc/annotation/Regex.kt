package frontier.skc.annotation

import org.intellij.lang.annotations.Language
import javax.annotation.RegEx

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Regex(@RegEx @Language("regexp") val regex: String)