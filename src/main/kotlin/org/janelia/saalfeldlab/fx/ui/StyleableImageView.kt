package org.janelia.saalfeldlab.fx.ui

import javafx.beans.property.ObjectProperty
import javafx.css.*
import javafx.scene.image.ImageView


class StyleableImageView() : ImageView(), Styleable {

    private val fitWidthProperty: ObjectProperty<Number?> = SimpleStyleableObjectProperty(WIDTH, this, "imageWidth")
    private val fitHeightProperty: ObjectProperty<Number?> = SimpleStyleableObjectProperty(HEIGHT, this, "imageWidth")


    init {
        styleClass += "styleable-image-view"
        fitWidthProperty.addListener { _, _, new -> new?.let { width -> fitWidth = width.toDouble() } }
        fitWidthProperty.addListener { _, _, new -> new?.let { height -> fitHeight = height.toDouble() } }
    }

    override fun getCssMetaData(): MutableList<CssMetaData<out Styleable, *>> {
        return getClassCssMetaData()
    }

    companion object {

        private val WIDTH = object : CssMetaData<StyleableImageView, Number>("-fit-width", StyleConverter.getSizeConverter()) {
            override fun isSettable(styleable: StyleableImageView) = !styleable.fitWidthProperty.isBound

            override fun getStyleableProperty(styleable: StyleableImageView): StyleableProperty<Number> {
                @Suppress("UNCHECKED_CAST")
                return styleable.fitWidthProperty as StyleableProperty<Number>
            }

            override fun getInitialValue(styleable: StyleableImageView): Number {
                return styleable.image.width
            }
        }

        private val HEIGHT = object : CssMetaData<StyleableImageView, Number>("-fit-height", StyleConverter.getSizeConverter()) {
            override fun isSettable(styleable: StyleableImageView) = !styleable.fitHeightProperty.isBound

            override fun getStyleableProperty(styleable: StyleableImageView): StyleableProperty<Number> {
                @Suppress("UNCHECKED_CAST")
                return styleable.fitHeightProperty as StyleableProperty<Number>
            }

            override fun getInitialValue(styleable: StyleableImageView): Number {
                return styleable.image.height
            }
        }

        private val STYLEABLES: MutableList<CssMetaData<out Styleable, *>> = mutableListOf<CssMetaData<out Styleable, *>>().also {
            it += ImageView.getClassCssMetaData()
            it += WIDTH
            it += HEIGHT
        }

        fun getClassCssMetaData(): MutableList<CssMetaData<out Styleable, *>> = STYLEABLES
    }
}