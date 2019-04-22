package frontier.skc.match

inline infix fun ParameterMatch.and(crossinline other: ParameterMatch): ParameterMatch = { type, annotations ->
    this(type, annotations) && other(type, annotations)
}

inline infix fun ParameterMatch.or(crossinline other: ParameterMatch): ParameterMatch = { type, annotations ->
    this(type, annotations) || other(type, annotations)
}

operator fun ParameterMatch.not(): ParameterMatch = { type, annotations ->
    !this(type, annotations)
}