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
package org.janelia.saalfeldlab.fx.extensions

import javafx.beans.binding.DoubleBinding
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableNumberValue
import javafx.scene.control.ContentDisplay
import javafx.scene.control.TitledPane
import javafx.scene.layout.Region

class TitledPaneExtensions {

	companion object {

		fun TitledPane.graphicsOnly(graphic: Region, arrowWidth: Double = 50.0) = graphicsOnly(graphic, SimpleDoubleProperty(arrowWidth))

		fun TitledPane.graphicsOnly(graphic: Region, arrowWidth: ObservableNumberValue): DoubleBinding {
			val regionWidth = this.widthProperty().subtract(arrowWidth)
			graphic.prefWidthProperty().bind(regionWidth)
			this.text = null
			this.graphic = graphic
			this.contentDisplay = ContentDisplay.GRAPHIC_ONLY
			return regionWidth
		}

		fun TitledPane.expandIfEnabled(isEnabled: ObservableBooleanValue) {
			isEnabled.addListener { _, _, new -> expandIfEnabled(new) }
			expandIfEnabled(isEnabled.value)
		}

		fun TitledPane.expandIfEnabled(isEnabled: Boolean) = if (isEnabled) enableAndExpand() else disableAndCollapse()

		fun TitledPane.enableAndExpand() {
			isCollapsible = true
			isExpanded = true
		}

		fun TitledPane.disableAndCollapse() {
			isExpanded = false
			isCollapsible = false
		}

	}

}
