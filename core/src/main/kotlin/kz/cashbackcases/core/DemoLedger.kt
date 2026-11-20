package kz.cashbackcases.core

sealed interface ActivationResult {
    data class Activated(val entry: CashbackEntry) : ActivationResult
    data object AlreadyPending : ActivationResult
    data class UnknownOffer(val offerId: String) : ActivationResult
    data class InvalidPurchase(val reason: String) : ActivationResult
}

sealed interface ConfirmResult {
    data class Confirmed(val entry: CashbackEntry) : ConfirmResult
    data object NotPending : ConfirmResult
    data object UnknownEntry : ConfirmResult
}

class DemoLedger(
    initial: LedgerState,
    private val clock: () -> Long,
    private val ids: () -> String,
) {
    private val lock = Any()
    private var entries: List<CashbackEntry> = initial.entries

    fun snapshot(): LedgerState = synchronized(lock) { LedgerState(entries) }

    fun activate(offerId: String, purchaseKzt: Long): ActivationResult = synchronized(lock) {
        val offer = SampleCatalog.offer(offerId) ?: return ActivationResult.UnknownOffer(offerId)
        val purchaseError = CashbackRules.purchaseError(purchaseKzt)
        if (purchaseError != null) return ActivationResult.InvalidPurchase(purchaseError)
        if (entries.any { it.offerId == offerId && it.status == CashbackStatus.PENDING }) {
            return ActivationResult.AlreadyPending
        }
        val entry = CashbackEntry(
            id = ids(),
            offerId = offer.id,
            merchantName = offer.merchantName,
            samplePurchaseKzt = purchaseKzt,
            sampleRatePercent = offer.sampleRatePercent,
            cashbackKzt = CashbackRules.cashbackKzt(purchaseKzt, offer.sampleRatePercent),
            status = CashbackStatus.PENDING,
            createdAtEpochMs = clock(),
            confirmedAtEpochMs = null,
            note = DemoCopy.ENTRY_NOTE,
        )
        entries = entries + entry
        ActivationResult.Activated(entry)
    }

    fun confirmLocally(entryId: String): ConfirmResult = synchronized(lock) {
        val index = entries.indexOfFirst { it.id == entryId }
        if (index < 0) return ConfirmResult.UnknownEntry
        val current = entries[index]
        if (current.status != CashbackStatus.PENDING) return ConfirmResult.NotPending
        val updated = current.copy(
            status = CashbackStatus.CONFIRMED,
            confirmedAtEpochMs = clock(),
        )
        entries = entries.toMutableList().also { it[index] = updated }
        ConfirmResult.Confirmed(updated)
    }

    fun replace(state: LedgerState) {
        synchronized(lock) {
            entries = state.entries
        }
    }
}
