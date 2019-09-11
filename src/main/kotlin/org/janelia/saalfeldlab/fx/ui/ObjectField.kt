package org.janelia.saalfeldlab.fx.ui

import java.io.File
import java.util.Arrays
import java.util.function.Predicate

import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.event.EventHandler
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.util.StringConverter
import java.util.function.Consumer

open class ObjectField<T, P : Property<T>>(
        value: P,
        private val converter: StringConverter<T>,
        vararg submitOn: SubmitOn) {

    enum class SubmitOn {
        ENTER_PRESSED,
        FOCUS_LOST
    }

    private val _value = value
    val textField = TextField()
    private val enterPressedHandler = EnterPressedHandler()
    private val focusLostHandler = FocusLostHandler()

    var value: T
        get() = _value.value
        set(value) = _value.setValue(value)

    class InvalidUserInput @JvmOverloads constructor(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

    init {
        value.addListener { _, _, newv -> textField.text = this.converter.toString(newv) }
        textField.text = this.converter.toString(this._value.value)
        submitOn.forEach { this.enableSubmitOn(it) }
    }

    fun valueProperty() = _value

    @Deprecated("Use getter syntax instead", ReplaceWith("getTextField()"))
    fun textField() = textField

    private fun enableSubmitOn(submitOn: SubmitOn) {
        when (submitOn) {
            SubmitOn.ENTER_PRESSED -> textField.addEventHandler(KeyEvent.KEY_PRESSED, enterPressedHandler)
            SubmitOn.FOCUS_LOST -> textField.focusedProperty().addListener(focusLostHandler)
        }
    }

    fun disableSubmitOn(submitOn: SubmitOn) {
        when (submitOn) {
            SubmitOn.ENTER_PRESSED -> textField.removeEventHandler(KeyEvent.KEY_PRESSED, enterPressedHandler)
            SubmitOn.FOCUS_LOST -> textField.focusedProperty().removeListener(focusLostHandler)
        }
    }

    fun submit() {
        try {
            value =converter.fromString(textField.text)
        } catch (e: InvalidUserInput) {
            textField.text = converter.toString(value)
        }

    }

    private inner class EnterPressedHandler : EventHandler<KeyEvent> {
        override fun handle(e: KeyEvent) {
            if (e.code == KeyCode.ENTER) {
                e.consume()
                submit()
            }
        }
    }

    private inner class FocusLostHandler : ChangeListener<Boolean> {

        override fun changed(observableValue: ObservableValue<out Boolean>, oldv: Boolean?, newv: Boolean?) = newv?.takeIf { it }?.let { submit() } ?: Unit

    }

    companion object {

        @JvmStatic
        fun fileField(
                initialFile: File,
                test: (File) -> Boolean,
                vararg submitOn: SubmitOn) = fileField(initialFile, Predicate { test(it) }, *submitOn)

        @JvmStatic
        fun fileField(
                initialFile: File,
                test: Predicate<File>,
                vararg submitOn: SubmitOn): ObjectField<File, Property<File>> {
            val converter = object : StringConverter<File>() {
                override fun toString(file: File): String {
                    return file.absolutePath
                }

                override fun fromString(s: String): File {
                    val f = File(s)
                    if (!test.test(f)) {
                        throw InvalidUserInput("User input could not converted to file: $s", null)
                    }
                    return f
                }
            }
            return ObjectField(SimpleObjectProperty(initialFile), converter, *submitOn)
        }

        @JvmStatic
        fun stringField(
                initialValue: String,
                vararg submitOn: SubmitOn): ObjectField<String, StringProperty> {

            val converter = object : StringConverter<String>() {
                override fun toString(s: String): String {
                    return s
                }

                override fun fromString(s: String): String {
                    return s
                }
            }

            return ObjectField(SimpleStringProperty(initialValue), converter, *submitOn)
        }
    }
}
