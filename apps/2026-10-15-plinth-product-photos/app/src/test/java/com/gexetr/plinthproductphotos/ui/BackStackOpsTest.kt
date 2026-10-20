package com.gexetr.plinthproductphotos.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The section switch once drained the stack with `while (isNotEmpty()) pop()` while `pop()` keeps
 * the root, so it spun forever on the main thread. The timeouts turn that hang into a failure.
 */
class BackStackOpsTest {

    @Test(timeout = 2_000)
    fun popKeepingRootNeverEmptiesTheStack() {
        val stack = mutableListOf("home")
        stack.popKeepingRoot()
        assertEquals(listOf("home"), stack)
    }

    @Test(timeout = 2_000)
    fun resetToRootDrainsDeepStackToExactlyTheNewRoot() {
        val stack = mutableListOf("home", "import", "autocard", "result")
        stack.resetToRoot("library")
        assertEquals(listOf("library"), stack)
    }

    @Test(timeout = 2_000)
    fun resetToRootMakesTheSectionTheFirstEntry() {
        // selectedSection reads firstOrNull(); [home, library] would keep Studio highlighted.
        val stack = mutableListOf("home")
        stack.resetToRoot("library")
        assertEquals("library", stack.first())
        assertEquals(1, stack.size)
    }

    @Test(timeout = 2_000)
    fun resetToSameRootFromRootIsANoOp() {
        val stack = mutableListOf("home")
        stack.resetToRoot("home")
        assertEquals(listOf("home"), stack)
    }

    @Test(timeout = 2_000)
    fun resetToRootOnAnEmptyStackSeedsIt() {
        val stack = mutableListOf<String>()
        stack.resetToRoot("home")
        assertEquals(listOf("home"), stack)
    }

    @Test(timeout = 2_000)
    fun pushUnlessTopRefusesADuplicateOfTheTop() {
        val stack = mutableListOf("home", "import")
        stack.pushUnlessTop("import")
        assertEquals(listOf("home", "import"), stack)
    }

    @Test(timeout = 2_000)
    fun replaceDetailKeepsAListPaneStackBounded() {
        val stack = mutableListOf("library")
        repeat(100) { i -> stack.replaceDetail(if (i % 2 == 0) "project-a" else "project-b") }
        assertEquals(listOf("library", "project-b"), stack)
    }
}
