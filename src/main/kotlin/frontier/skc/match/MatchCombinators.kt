package frontier.skc.match

inline infix fun ParameterMatch.and(crossinline other: ParameterMatch): ParameterMatch = {
    this(it) && other(it)
}

inline infix fun ParameterMatch.or(crossinline other: ParameterMatch): ParameterMatch = {
    this(it) || other(it)
}

operator fun ParameterMatch.not(): ParameterMatch = {
    !this(it)
}