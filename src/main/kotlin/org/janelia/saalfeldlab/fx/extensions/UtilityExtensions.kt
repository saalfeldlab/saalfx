package org.janelia.saalfeldlab.fx.extensions

import javafx.util.Pair
import java.util.*

val <T> Optional<T>.nullable: T?
	get() = orElse(null)

operator fun <A, B> Pair<A, B>.component1() = key
operator fun <A, B> Pair<A, B>.component2() = value
