package frontier.skc.annotation

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER)
annotation class Permission(val value: String)