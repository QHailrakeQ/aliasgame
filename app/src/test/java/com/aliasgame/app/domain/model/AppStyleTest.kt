package com.aliasgame.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppStyleTest {

    @Test
    fun `every style is restored from its stored id`() {
        AppStyle.entries.forEach { style -> assertEquals(style, AppStyle.fromId(style.id)) }
    }

    @Test
    fun `nothing stored means the default style`() {
        assertEquals(AppStyle.DEFAULT, AppStyle.fromId(null))
    }

    @Test
    fun `an unknown stored value falls back to the default style`() {
        assertEquals(AppStyle.DEFAULT, AppStyle.fromId("disco"))
    }

    @Test
    fun `stored ids are unique`() {
        assertEquals(AppStyle.entries.size, AppStyle.entries.map { it.id }.toSet().size)
    }
}
