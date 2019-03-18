package frontier.skc.annotation

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Flag(vararg val specs: String)