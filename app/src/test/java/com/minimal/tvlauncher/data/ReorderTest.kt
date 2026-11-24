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
    fun reorderWithin_movesOnlyAmongTheSectionItems_leavingTheOthersInPlace() {
        val all = listOf("a", "x", "b", "y", "c") // a,b,c son de la sección; x,y no
        val inSection = { s: String -> s in setOf("a", "b", "c") }
        assertEquals(listOf("c", "x", "a", "y", "b"), reorderWithin(all, inSection, from = 2, to = 0))
        assertEquals(listOf("b", "x", "a", "y", "c"), reorderWithin(all, inSection, from = 0, to = 1))
        assertEquals(all, reorderWithin(all, inSection, from = 0, to = 5))
        assertEquals(all, reorderWithin(all, inSection, from = -1, to = 1))
    }

    @Test
    fun toggleFavorite_addsRemoves_andRespectsTheMaximum() {
        assertEquals(setOf("a"), toggleFavorite(emptySet(), "a", max = 2))
        assertEquals(setOf("b"), toggleFavorite(setOf("a", "b"), "a", max = 2))
        assertEquals(setOf("a", "b"), toggleFavorite(setOf("a", "b"), "c", max = 2)) // lleno: no agrega
    }

    @Test
    fun toggleHidden_addsWhenAbsent_andRemovesWhenPresent() {
        assertEquals(setOf("x"), toggleHidden(emptySet(), "x"))
        assertEquals(setOf("y"), toggleHidden(setOf("x", "y"), "x"))
    }
}
