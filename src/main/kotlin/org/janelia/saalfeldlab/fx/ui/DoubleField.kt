package org.janelia.saalfeldlab.fx.ui

import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.event.Event
import javafx.event.EventHandler
import javafx.scene.control.TextField
import javafx.util.StringConverter

class DoubleField(initialValue: Double) {
    private val field = TextField()

    private val valueAsString = SimpleStringProperty()

    private val value = SimpleDoubleProperty()

    init {

        valueAsString.addListener { obs, oldv, newv -> this.field.text = newv }

        valueAsString.bindBidirectional(value, object : StringConverter<Number>() {
            override fun toString(value: Number): String {
                return value.toString()
            }

            override fun fromString(string: String): Double? {
                try {
                    return java.lang.Double.valueOf(string)
                } catch (e: NumberFormatException) {
                    return value.get()
                } catch (e: NullPointerException) {
                    return value.get()
                }

            }
        })

        this.value.set(initialValue)

        this.field.onAction = wrapAsEventHandler(Runnable { this.submitText() })
        this.field.focusedProperty().addListener { obs, oldv, newv -> submitText(!newv) }

    }

    fun textField(): TextField {
        return this.field
    }

    fun valueProperty(): DoubleProperty {
        return this.value
    }

    private fun <E : Event> wrapAsEventHandler(r: Runnable): EventHandler<E> {
        return wrapAsEventHandler(r, true)
    }

    private fun <E : Event> wrapAsEventHandler(r: Runnable, consume: Boolean): EventHandler<E> {
        return EventHandler { e ->
            if (consume)
                e.consume()
            r.run()
        }
    }

    private fun submitText(submit: Boolean = true) {
        if (!submit)
            return
        try {
            val `val` = java.lang.Double.valueOf(textField().text)
            value.set(`val`)
        } catch (e: NumberFormatException) {
            this.field.text = this.valueAsString.get()
        } catch (e: NullPointerException) {
            this.field.text = this.valueAsString.get()
        }

    }
}
