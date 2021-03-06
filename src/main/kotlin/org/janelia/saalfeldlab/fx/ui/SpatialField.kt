/*-
 * #%L
 * Saalfeld lab JavaFX tools and extensions
 * %%
 * Copyright (C) 2019 Philipp Hanslovsky, Stephan Saalfeld
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.janelia.saalfeldlab.fx.ui

import java.util.function.DoublePredicate
import java.util.function.IntPredicate
import java.util.function.LongPredicate

import javafx.beans.property.DoubleProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.LongProperty
import javafx.beans.property.Property
import javafx.scene.Node
import javafx.scene.layout.HBox
import java.util.function.ToDoubleFunction
import java.util.function.ToIntFunction
import java.util.function.ToLongFunction

class SpatialField<P : Property<Number>> private constructor(
        val x: NumberField<P>,
        val y: NumberField<P>,
        val z: NumberField<P>,
        textFieldWidth: Double) {

    val node: Node

    init {
        x.textField.promptText = "X"
        y.textField.promptText = "Y"
        z.textField.promptText = "Z"
        x.textField.prefWidth = textFieldWidth
        y.textField.prefWidth = textFieldWidth
        z.textField.prefWidth = textFieldWidth
        this.node = HBox(x.textField, y.textField, z.textField)
    }

    fun getAs(xyz: LongArray, converter: (Number) -> Long = { it.toLong() }) = getAs(xyz, ToLongFunction { converter(it) })

    @JvmOverloads
    fun getAs(xyz: LongArray, converter: ToLongFunction<Number> = ToLongFunction{  it.toLong() }): LongArray {
        xyz[0] = x.valueProperty().value.let { converter.applyAsLong(it) }
        xyz[1] = y.valueProperty().value.let { converter.applyAsLong(it) }
        xyz[2] = z.valueProperty().value.let { converter.applyAsLong(it) }
        return xyz
    }

    fun getAs(xyz: IntArray, converter: (Number) -> Int = { it.toInt() }) = getAs(xyz, ToIntFunction { converter(it) })

    @JvmOverloads
    fun getAs(xyz: IntArray, converter: ToIntFunction<Number> = ToIntFunction{  it.toInt() }): IntArray {
        xyz[0] = x.valueProperty().value.let { converter.applyAsInt(it) }
        xyz[1] = y.valueProperty().value.let { converter.applyAsInt(it) }
        xyz[2] = z.valueProperty().value.let { converter.applyAsInt(it) }
        return xyz
    }

    fun getAs(xyz: DoubleArray, converter: (Number) -> Double = { it.toDouble() }) = getAs(xyz, ToDoubleFunction { converter(it) })

    @JvmOverloads
    fun getAs(xyz: DoubleArray, converter: ToDoubleFunction<Number> = ToDoubleFunction{  it.toDouble() }): DoubleArray {
        xyz[0] = x.valueProperty().value.let { converter.applyAsDouble(it) }
        xyz[1] = y.valueProperty().value.let { converter.applyAsDouble(it) }
        xyz[2] = z.valueProperty().value.let { converter.applyAsDouble(it) }
        return xyz
    }

    companion object {

        @JvmStatic
        fun doubleField(
                initialValue: Double,
                test: DoublePredicate,
                textFieldWidth: Double,
                vararg submitOn: ObjectField.SubmitOn): SpatialField<DoubleProperty> {
            return SpatialField(
                    NumberField.doubleField(initialValue, test, *submitOn),
                    NumberField.doubleField(initialValue, test, *submitOn),
                    NumberField.doubleField(initialValue, test, *submitOn),
                    textFieldWidth
            )
        }

        @JvmStatic
        fun intField(
                initialValue: Int,
                test: IntPredicate,
                textFieldWidth: Double,
                vararg submitOn: ObjectField.SubmitOn): SpatialField<IntegerProperty> {
            return SpatialField(
                    NumberField.intField(initialValue, test, *submitOn),
                    NumberField.intField(initialValue, test, *submitOn),
                    NumberField.intField(initialValue, test, *submitOn),
                    textFieldWidth
            )
        }

        @JvmStatic
        fun longField(
                initialValue: Long,
                test: LongPredicate,
                textFieldWidth: Double,
                vararg submitOn: ObjectField.SubmitOn): SpatialField<LongProperty> {
            return SpatialField(
                    NumberField.longField(initialValue, test, *submitOn),
                    NumberField.longField(initialValue, test, *submitOn),
                    NumberField.longField(initialValue, test, *submitOn),
                    textFieldWidth
            )
        }
    }
}
