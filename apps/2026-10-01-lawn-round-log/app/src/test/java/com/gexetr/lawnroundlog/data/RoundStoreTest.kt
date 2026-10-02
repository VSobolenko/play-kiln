package com.gexetr.lawnroundlog.data

import com.gexetr.lawnroundlog.domain.Balance
import com.gexetr.lawnroundlog.domain.Customer
import com.gexetr.lawnroundlog.domain.Payment
import com.gexetr.lawnroundlog.domain.Round
import com.gexetr.lawnroundlog.domain.RoundState
import com.gexetr.lawnroundlog.domain.Visit
import com.gexetr.lawnroundlog.domain.VisitStatus
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The persistence promises: the record and every balance survive a restart, the operator's order
 * survives it too, and a file that cannot be read costs the app nothing but the record it could not
 * read — never the ability to open.
 *
 * The sandbox is under the module's own `build/` directory rather than the system temporary
 * directory, so a test run writes nothing outside the repository
 * (`.cursor/rules/workspace-hygiene.mdc`). Gradle runs unit tests with the module directory as the
 * working directory, so `build/tmp/...` resolves inside the slot.
 */
class RoundStoreTest {

    @Test
    fun everyBalanceIsIdenticalAfterASaveAndLoadRoundTrip() {
        val ada = Customer(id = "ada", name = "Ada Reyes", position = 0)
        val state = RoundState(
            customers = listOf(ada),
            visits = listOf(
                Visit(Round.visitId("ada", "2026-10-01"), "ada", "2026-10-01", VisitStatus.MOWED, 40.0),
                Visit(Round.visitId("ada", "2026-10-08"), "ada", "2026-10-08", VisitStatus.MOWED, 40.0),
                Visit(Round.visitId("ada", "2026-10-15"), "ada", "2026-10-15", VisitStatus.SKIPPED, 0.0),
            ),
            payments = listOf(
                Payment(Round.paymentId("ada", "2026-10-09"), "ada", "2026-10-09", 40.0),
            ),
        )
        val file = storeFile("restart-identity")

        RoundStore(file).save(state)
        val restarted = RoundStore(file).load()

        assertEquals(state, restarted)
        assertEquals(40.0, Balance.balanceFor(state, "ada"), 0.0001)
        assertEquals(
            Balance.balanceFor(state, "ada"),
            Balance.balanceFor(restarted, "ada"),
            0.0001,
        )
        assertEquals(Balance.totalOwed(state), Balance.totalOwed(restarted), 0.0001)
    }

    @Test
    fun theOperatorsOrderSurvivesASaveAndLoadRoundTrip() {
        val state = RoundState(
            customers = listOf(
                Customer(id = "a", name = "A", position = 0, createdAt = 1L),
                Customer(id = "b", name = "B", position = 1, createdAt = 2L),
                Customer(id = "c", name = "C", position = 2, createdAt = 3L),
            ),
        )
        val file = storeFile("order-survives-restart")

        val moved = Round.move(state, "c", 1)
        RoundStore(file).save(moved)
        val restarted = RoundStore(file).load()

        assertEquals(listOf("a", "c", "b"), Round.ordered(moved).map { it.id })
        assertEquals(listOf("a", "c", "b"), Round.ordered(restarted).map { it.id })
    }

    @Test
    fun aCorruptFileReadsAsAnEmptyRecordWithoutThrowing() {
        val file = storeFile("corrupt")
        file.writeText("{ this is not the document it claims to be")

        val loaded = RoundStore(file).load()

        assertEquals(RoundState(), loaded)
    }

    @Test
    fun aFileThatIsTheWrongShapeEntirelyReadsAsAnEmptyRecord() {
        val file = storeFile("wrong-shape")
        file.writeText("[1, 2, 3]")

        assertEquals(RoundState(), RoundStore(file).load())
    }

    @Test
    fun aMissingFileReadsAsAnEmptyRecordWithoutThrowing() {
        val file = storeFile("missing")

        assertTrue(!file.exists())
        assertEquals(RoundState(), RoundStore(file).load())
    }

    @Test
    fun saveCreatesTheDocumentAndLeavesNoTemporaryFileBehind() {
        val file = storeFile("atomic-write")
        val state = RoundState(customers = listOf(Customer(id = "ada", name = "Ada Reyes")))

        RoundStore(file).save(state)

        assertTrue(file.isFile)
        assertEquals(state, RoundStore(file).load())
        val leftovers = file.parentFile?.listFiles()?.filter { it.name.endsWith(".tmp") }.orEmpty()
        assertEquals(emptyList<File>(), leftovers)
    }

    /**
     * A directory for one test, under the module's `build/`, with no stale document in it.
     *
     * Each test names its own directory so the tests cannot see one another's files, and the file
     * itself is deleted before the test runs so a previous run cannot make a "missing file" test
     * pass or fail for the wrong reason.
     */
    private fun storeFile(name: String): File {
        val directory = File("build/tmp/round-store-test", name)
        directory.mkdirs()
        val file = File(directory, "round-log.json")
        file.delete()
        return file
    }
}
