package frontier.skc.annotation

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class Command(vararg val aliases: String)
