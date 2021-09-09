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

import com.sun.javafx.application.PlatformImpl
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.DoubleProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.LongProperty
import javafx.beans.property.LongPropertyBase
import javafx.beans.property.Property
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.Scene
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.util.StringConverter
import java.util.function.DoublePredicate
import java.util.function.IntPredicate
import java.util.function.LongPredicate

class NumberField<P : Property<Number>>(
        value: P,
        converter: StringConverter<Number>,
        vararg submitOn: SubmitOn) : ObjectField<Number, P>(value, converter, *submitOn) {
    companion object {

        @JvmStatic
        fun doubleField(
                initialValue: Double,
                test: DoublePredicate,
                vararg submitOn: SubmitOn): NumberField<DoubleProperty> {
            val converter = object : StringConverter<Number>() {
                override fun toString(number: Number) = number.toDouble().toString()

                override fun fromString(s: String): Number {
                    try {
                        val value = java.lang.Double.parseDouble(s)
                        if (!test.test(value))
                            throw InvalidUserInput("Illegal value: $s")
                        return value
                    } catch (e: NumberFormatException) {
                        throw InvalidUserInput("Unable to convert: $s", e)
                    }

                }
            }

            return NumberField(SimpleDoubleProperty(initialValue), converter, *submitOn)
        }

        @JvmStatic
        fun longField(initialValue: Long, test: LongPredicate, vararg submitOn: SubmitOn): NumberField<LongProperty> {
            val converter = object : StringConverter<Number>() {
                override fun toString(number: Number) =number.toLong().toString()

                override fun fromString(s: String): Number {
                    try {
                        val value = java.lang.Long.parseLong(s)
                        if (!test.test(value))
                            throw InvalidUserInput("Illegal value: $s")
                        return value
                    } catch (e: NumberFormatException) {
                        throw InvalidUserInput("Unable to convert: $s", e)
                    }

                }
            }

            val lp = object : LongPropertyBase(initialValue) {
                override fun getBean(): Any? {
                    return null
                }

                override fun getName(): String {
                    return ""
                }
            }

            return NumberField(lp, converter, *submitOn)
        }

        @JvmStatic
        fun intField(
                initialValue: Int,
                test: IntPredicate,
                vararg submitOn: SubmitOn): NumberField<IntegerProperty> {
            val converter = object : StringConverter<Number>() {
                override fun toString(number: Number): String {
                    return java.lang.Long.toString(number.toInt().toLong())
                }

                override fun fromString(s: String): Int {
                    try {
                        val `val` = Integer.parseInt(s)
                        if (!test.test(`val`))
                            throw InvalidUserInput("Illegal value: $s")
                        return `val`
                    } catch (e: NumberFormatException) {
                        throw InvalidUserInput("Unable to convert: $s", e)
                    }

                }
            }

            return NumberField(SimpleIntegerProperty(initialValue), converter, *submitOn)
        }

        @JvmStatic
        fun main(args: Array<String>) {
            PlatformImpl.startup { }

            val df = doubleField(
                    5.0,
                    { _ -> true },
                    SubmitOn.ENTER_PRESSED,
                    SubmitOn.FOCUS_LOST
            )
            val lbl1 = TextField()
            val converted1 = Bindings.convert(df.valueProperty())
            lbl1.textProperty().bind(converted1)
            val hb1 = HBox(df.textField, lbl1)

            val lf = longField(
                    4,
                    { _ -> true },
                    SubmitOn.ENTER_PRESSED,
                    SubmitOn.FOCUS_LOST
            )
            val lbl2 = TextField()
            val converted2 = Bindings.convert(lf.valueProperty())
            lbl2.textProperty().bind(converted2)
            val hb2 = HBox(lf.textField, lbl2)

            val ulf = longField(
                    4,
                    { d -> d >= 0 },
                    SubmitOn.ENTER_PRESSED,
                    SubmitOn.FOCUS_LOST
            )
            val lbl3 = TextField()
            val converted3 = Bindings.convert(ulf.valueProperty())
            lbl3.textProperty().bind(converted3)
            val hb3 = HBox(ulf.textField, lbl3)


            Platform.runLater {
                val pane = VBox(hb1, hb2, hb3)
                val scene = Scene(pane)
                val stage = Stage()
                stage.scene = scene
                stage.show()
            }
        }
    }
}
