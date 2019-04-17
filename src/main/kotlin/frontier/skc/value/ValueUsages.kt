package frontier.skc.value

import org.spongepowered.api.text.Text

object ValueUsages {

    val EMPTY: AnnotatedValueUsage = { _, _ ->
        Text.EMPTY
    }

    val IDENTITY: AnnotatedValueUsage = { _, key ->
        key
    }

    val SINGLE: AnnotatedValueUsage = { _, key ->
        Text.of("<", key, ">")
    }

    val VARIADIC: AnnotatedValueUsage = { _, key ->
        Text.of("<", key, "...>")
    }
}