package com.minimal.tvlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReorderTest {
    private val order = listOf("a", "b", "c", "d")

    @Test
    fun moveItem_movesToStart_toEnd_andMiddle() {
        assertEquals(listOf("c", "a", "b", "d"), moveItem(order, from = 2, to = 0))
        assertEquals(listOf("b", "c", "d", "a"), moveItem(order, from = 0, to = 3))
        assertEquals(listOf("a", "c", "b", "d"), moveItem(order, from = 1, to = 2))
    }

    @Test
    fun moveItem_leavesListUnchanged_whenIndicesAreOutOfRangeOrEqual() {
        assertEquals(order, moveItem(order, from = -1, to = 2))
        assertEquals(order, moveItem(order, from = 1, to = 9))
        assertEquals(order, moveItem(order, from = 9, to = 0))
        assertEquals(order, moveItem(order, from = 2, to = 2))
        assertEquals(emptyList<String>(), moveItem(emptyList<String>(), from = 0, to = 0))
    }

    @Test
    fun moveItem_neverLosesOrDuplicatesElements() {
        val moved = moveItem(order, from = 1, to = 3)
        assertEquals(order.sorted(), moved.sorted())
    }

    @Test
    fun toggleHidden_addsWhenAbsent_andRemovesWhenPresent() {
        assertEquals(setOf("x"), toggleHidden(emptySet(), "x"))
        assertEquals(setOf("y"), toggleHidden(setOf("x", "y"), "x"))
    }
}
