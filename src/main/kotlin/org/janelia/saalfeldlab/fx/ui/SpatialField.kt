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

import javafx.beans.property.DoubleProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.LongProperty
import javafx.beans.property.Property
import javafx.scene.Node
import javafx.scene.layout.HBox
import java.util.function.DoublePredicate
import java.util.function.IntPredicate
import java.util.function.LongPredicate

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

    fun asLongArray() = LongArray(3).apply {
        this[0] = x.value.toLong()
        this[1] = y.value.toLong()
        this[2] = z.value.toLong()
    }

    fun asIntArray() = IntArray(3).apply {
        this[0] = x.value.toInt()
        this[1] = y.value.toInt()
        this[2] = z.value.toInt()
    }

    fun asDoubleArray() = DoubleArray(3).apply {
        this[0] = x.value.toDouble()
        this[1] = y.value.toDouble()
        this[2] = z.value.toDouble()
    }

    companion object {

        @JvmStatic
        fun doubleField(
            initialValue: Double,
            test: DoublePredicate,
            textFieldWidth: Double,
            vararg submitOn: ObjectField.SubmitOn
        ): SpatialField<DoubleProperty> {
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
