package org.janelia.saalfeldlab.fx.extensions

import javafx.beans.property.SimpleDoubleProperty
import org.junit.Test
import kotlin.test.assertEquals

class ExtensionsTest {

    @Test
    fun testOnSucess() {

        class DelegateTest {

            val firstRowHeightProperty = SimpleDoubleProperty(50.0)
            var firstRowHeight by firstRowHeightProperty.nonnull()
        }

        val dt = DelegateTest()
        val dt2 = DelegateTest()
        assertEquals(50.0, dt.firstRowHeightProperty.value)
        assertEquals(dt.firstRowHeightProperty.value, dt.firstRowHeight)

        dt.firstRowHeight = 10.0
        assertEquals(10.0, dt.firstRowHeightProperty.value)
        assertEquals(dt.firstRowHeightProperty.value, dt.firstRowHeight)

        dt.firstRowHeight = dt2.firstRowHeight
        dt.firstRowHeight = 100.0
        assertEquals(100.0, dt.firstRowHeightProperty.value)
        assertEquals(dt.firstRowHeightProperty.value, dt.firstRowHeight)


    }
}
