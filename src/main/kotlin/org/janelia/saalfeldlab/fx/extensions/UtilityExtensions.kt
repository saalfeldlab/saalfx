package org.janelia.saalfeldlab.fx.extensions

import javafx.util.Pair
import java.util.Optional

@Deprecated("prefer getOrNull()", replaceWith = ReplaceWith("getOrNull()", "kotlin.jvm.optionals.getOrNull"))
val <T> Optional<T>.nullable: T?
	get() = orElse(null)

operator fun <A, B> Pair<A, B>.component1(): A? = key
operator fun <A, B> Pair<A, B>.component2(): B? = value
