package frontier.skc.value

object ValueCompleters {

    val EMPTY: AnnotatedValueCompleter = { _, _, _ ->
        emptyList()
    }
}