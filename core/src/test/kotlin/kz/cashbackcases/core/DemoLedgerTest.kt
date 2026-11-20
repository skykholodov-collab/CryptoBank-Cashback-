package kz.cashbackcases.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class DemoLedgerTest {
    @Test
    fun formatsTengeWithSpaces() {
        assertEquals("0 ₸", formatKzt(0))
        assertEquals("840 ₸", formatKzt(840))
        assertEquals("1 000 ₸", formatKzt(1_000))
        assertEquals("12 000 ₸", formatKzt(12_000))
        assertEquals("2 750 ₸", formatKzt(2_750))
        assertEquals("2 975 ₸", formatKzt(2_975))
        assertEquals("3 590 ₸", formatKzt(3_590))
        assertEquals("100 000 ₸", formatKzt(100_000))
        assertEquals("1 000 000 ₸", formatKzt(1_000_000))
        assertEquals("−840 ₸", formatKzt(-840))
    }

    @Test
    fun floorsFractionalTenge() {
        assertEquals(30L, CashbackRules.cashbackKzt(1_001L, 3))
        assertEquals(1_000L, CashbackRules.cashbackKzt(10_000L, 10))
    }

    @Test
    fun rejectsRateAboveSampleCap() {
        try {
            CashbackRules.cashbackKzt(10_000L, 11)
            throw AssertionError("rate 11 must fail")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("10"))
        }
    }

    @Test
    fun catalogIsThreeFictionalCollectionsCappedAtTenPercent() {
        assertEquals(listOf("city", "home", "service"), SampleCatalog.collections.map { it.id })
        assertEquals(listOf("Город", "Дом и техника", "Сервис"), SampleCatalog.collections.map { it.title })
        val offers = SampleCatalog.collections.flatMap { it.offers }
        assertEquals(9, offers.size)
        assertTrue(offers.all { it.sampleRatePercent in 1..10 })
        assertEquals(listOf("tulpar-travel"), offers.filter { it.sampleRatePercent == 10 }.map { it.id })
        assertEquals(
            setOf(
                "Алма Маркет",
                "Степь Аптека",
                "Дом Кофе",
                "Номад Тех",
                "Юрта Мебель",
                "Байкал Быт",
                "Тулпар Тревел",
                "Арлан Фитнес",
                "Сарыарка Кино",
            ),
            offers.map { it.merchantName }.toSet(),
        )
        offers.forEach { offer ->
            assertTrue(offer.blurb.startsWith("Вымышленн"))
            assertTrue(offer.terms.any { "вымышл" in it.lowercase() })
            assertTrue(offer.terms.any { "комиссии продавца" in it })
        }
    }

    @Test
    fun seedSplitsPendingAndConfirmed() {
        val state = DemoSeed.state()
        assertEquals(840L, state.pendingKzt)
        assertEquals(225L, state.confirmedKzt)
        assertEquals(CashbackStatus.CONFIRMED, state.entries.first { it.id == "seed-confirmed-coffee" }.status)
        assertEquals(CashbackStatus.PENDING, state.entries.first { it.id == "seed-pending-tech" }.status)
    }

    @Test
    fun activationStaysPendingUntilLocalConfirm() {
        var now = 10L
        val ledger = DemoLedger(DemoSeed.state(), clock = { now }, ids = { "user-1" })
        val blocked = ledger.activate("nomad-tech", 4_500L)
        assertTrue(blocked is ActivationResult.AlreadyPending)
        assertEquals(2, ledger.snapshot().entries.size)

        now = 80L
        val created = ledger.activate("tulpar-travel", 27_500L)
        assertTrue(created is ActivationResult.Activated)
        val entry = (created as ActivationResult.Activated).entry
        assertEquals(CashbackStatus.PENDING, entry.status)
        assertEquals(2_750L, entry.cashbackKzt)
        assertNull(entry.confirmedAtEpochMs)
        assertEquals(840L + 2_750L, ledger.snapshot().pendingKzt)
        assertEquals(225L, ledger.snapshot().confirmedKzt)

        val again = ledger.activate("tulpar-travel", 12_000L)
        assertTrue(again is ActivationResult.AlreadyPending)

        now = 90L
        val confirmed = ledger.confirmLocally(entry.id)
        assertTrue(confirmed is ConfirmResult.Confirmed)
        assertEquals(90L, (confirmed as ConfirmResult.Confirmed).entry.confirmedAtEpochMs)
        assertEquals(840L, ledger.snapshot().pendingKzt)
        assertEquals(225L + 2_750L, ledger.snapshot().confirmedKzt)

        val repeat = ledger.confirmLocally(entry.id)
        assertTrue(repeat is ConfirmResult.NotPending)
        assertEquals(225L + 2_750L, ledger.snapshot().confirmedKzt)

        val second = ledger.activate("tulpar-travel", 4_500L) as ActivationResult.Activated
        assertEquals(450L, second.entry.cashbackKzt)
        assertEquals(CashbackStatus.PENDING, second.entry.status)
    }

    @Test
    fun invalidPurchaseAndUnknownIdsDoNotMutate() {
        val ledger = DemoLedger(DemoSeed.state(), clock = { 1L }, ids = { "x" })
        val before = ledger.snapshot()
        assertTrue(ledger.activate("missing-shop", 4_500L) is ActivationResult.UnknownOffer)
        assertTrue(ledger.activate("dom-kofe", 999L) is ActivationResult.InvalidPurchase)
        assertTrue(ledger.activate("dom-kofe", 100_001L) is ActivationResult.InvalidPurchase)
        assertTrue(ledger.activate("dom-kofe", 0L) is ActivationResult.InvalidPurchase)
        assertTrue(ledger.confirmLocally("missing") is ConfirmResult.UnknownEntry)
        assertTrue(ledger.confirmLocally("seed-confirmed-coffee") is ConfirmResult.NotPending)
        assertEquals(before, ledger.snapshot())
    }

    @Test
    fun withdrawalRequestDoesNotChangeEntries() {
        val state = DemoSeed.state()
        val decision = WithdrawalPolicy.request(state)
        assertFalse(WithdrawalPolicy.availableInDemo)
        assertTrue(decision is WithdrawalDecision.Unavailable)
        assertEquals(state.entries, decision.unchanged.entries)
        assertEquals(state.pendingKzt, decision.unchanged.pendingKzt)
        assertTrue(decision.message.contains("недоступен"))
        assertTrue(decision.message.contains("не отправляет деньги"))
        val names = DemoLedger::class.java.declaredMethods.map { it.name.lowercase() }
        assertTrue(names.none { "withdraw" in it || "payout" in it || "deposit" in it })
    }

    @Test
    fun dialogCopyNamesPurchaseAndRefusesPayment() {
        val text = DemoCopy.activationDialog(12_000L, 840L, 7)
        assertTrue(text.contains("12 000 ₸"))
        assertTrue(text.contains("840 ₸"))
        assertTrue(text.contains("7%"))
        assertTrue(text.contains("Деньги не списываются и не отправляются."))
        assertTrue(DemoCopy.PURCHASE_HINT.contains("покупки"))
        assertFalse(DemoCopy.CALC_DISCLAIMER.lowercase().contains("гарантир"))
    }

    @Test
    fun repositoryPersistsReloadAndReset() {
        val dir = Files.createTempDirectory("cashback-ledger").toFile()
        val file = dir.resolve("demo-ledger.json")
        val first = AppRepository(
            storage = FileLedgerStorage(file),
            clock = { 1_000L },
            ids = { "saved-1" },
        )
        assertEquals(840L, first.state.value.pendingKzt)
        val activated = first.activate("alma-market", 12_000L) as ActivationResult.Activated
        assertEquals(360L, activated.entry.cashbackKzt)
        assertEquals(840L + 360L, first.state.value.pendingKzt)

        val reloaded = AppRepository(
            storage = FileLedgerStorage(file),
            clock = { 2_000L },
            ids = { "saved-2" },
        )
        assertEquals(listOf("seed-confirmed-coffee", "seed-pending-tech", "saved-1"), reloaded.state.value.entries.map { it.id })
        val confirmed = reloaded.confirmLocally("saved-1") as ConfirmResult.Confirmed
        assertEquals(CashbackStatus.CONFIRMED, confirmed.entry.status)
        assertEquals(2_000L, confirmed.entry.confirmedAtEpochMs)

        val afterConfirm = AppRepository(FileLedgerStorage(file), clock = { 3L }, ids = { "saved-3" })
        assertEquals(840L, afterConfirm.state.value.pendingKzt)
        assertEquals(225L + 360L, afterConfirm.state.value.confirmedKzt)

        afterConfirm.reset()
        assertEquals(DemoSeed.state().entries.map { it.id }, afterConfirm.state.value.entries.map { it.id })
        val afterReset = AppRepository(FileLedgerStorage(file), clock = { 4L }, ids = { "saved-4" })
        assertEquals(840L, afterReset.state.value.pendingKzt)
        assertEquals(225L, afterReset.state.value.confirmedKzt)
    }

    @Test
    fun corruptFileFallsBackToSeed() {
        val dir = Files.createTempDirectory("cashback-corrupt").toFile()
        val file = dir.resolve("demo-ledger.json")
        file.writeText("{not-json")
        assertNull(FileLedgerStorage(file).read())
        val repo = AppRepository(FileLedgerStorage(file), clock = { 5L }, ids = { "n" })
        assertEquals(DemoSeed.state().entries.map { it.id }, repo.state.value.entries.map { it.id })
        assertTrue(file.readText().contains("seed-pending-tech"))
        assertTrue(file.readText().contains("PENDING"))
        assertTrue(file.readText().contains("CONFIRMED"))
    }
}
