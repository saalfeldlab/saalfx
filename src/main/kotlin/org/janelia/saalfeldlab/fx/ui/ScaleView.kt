package org.janelia.saalfeldlab.fx.ui

import javafx.beans.property.ObjectProperty
import javafx.css.*
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.layout.Region


open class ScaleView protected constructor() : Pane(), Styleable {

	protected val paneWidthProperty: ObjectProperty<Number?> = SimpleStyleableObjectProperty(WIDTH, this, "paneWidth")
	protected val paneHeightProperty: ObjectProperty<Number?> = SimpleStyleableObjectProperty(HEIGHT, this, "paneHeight")


	init {
		styleClass += "scale-pane"
	}

	constructor(child: Node = Region()) : this() {
		children += child
		paneWidthProperty.addListener { _, _, new -> new?.let { width ->
			prefWidth = width.toDouble()
			(child as? Region)?.minWidthProperty()?.bind(paneWidthProperty)
		} }
		paneHeightProperty.addListener { _, _, new -> new?.let { height ->
			prefHeight = height.toDouble()
			(child as? Region)?.minHeightProperty()?.bind(paneHeightProperty)
		} }
	}

	override fun getCssMetaData(): MutableList<CssMetaData<out Styleable, *>> {
		return getClassCssMetaData()
	}

	companion object {

		private val WIDTH = object : CssMetaData<ScaleView, Number>("-pref-width", StyleConverter.getSizeConverter()) {
			override fun isSettable(styleable: ScaleView) = !styleable.paneWidthProperty.isBound

			override fun getStyleableProperty(styleable: ScaleView): StyleableProperty<Number> {
				@Suppress("UNCHECKED_CAST")
				return styleable.paneWidthProperty as StyleableProperty<Number>
			}

			override fun getInitialValue(styleable: ScaleView): Number {
				return styleable.prefWidth
			}
		}

		private val HEIGHT = object : CssMetaData<ScaleView, Number>("-pref-height", StyleConverter.getSizeConverter()) {
			override fun isSettable(styleable: ScaleView) = !styleable.paneHeightProperty.isBound

			override fun getStyleableProperty(styleable: ScaleView): StyleableProperty<Number> {
				@Suppress("UNCHECKED_CAST")
				return styleable.paneHeightProperty as StyleableProperty<Number>
			}

			override fun getInitialValue(styleable: ScaleView): Number {
				return styleable.prefHeight
			}
		}

		private val STYLEABLES: MutableList<CssMetaData<out Styleable, *>> = mutableListOf<CssMetaData<out Styleable, *>>().also {
			it += Region.getClassCssMetaData()
			it += WIDTH
			it += HEIGHT
		}

		fun getClassCssMetaData(): MutableList<CssMetaData<out Styleable, *>> = STYLEABLES
	}
}