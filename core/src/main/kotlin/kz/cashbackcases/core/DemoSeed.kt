package kz.cashbackcases.core

object DemoSeed {
    const val COFFEE_CREATED_EPOCH_MS = 1_793_511_000_000L
    const val COFFEE_CONFIRMED_EPOCH_MS = 1_793_530_800_000L
    const val TECH_CREATED_EPOCH_MS = 1_793_870_100_000L

    fun state(): LedgerState = LedgerState(
        entries = listOf(
            entry(
                id = "seed-confirmed-coffee",
                offer = SampleCatalog.requireOffer("dom-kofe"),
                purchaseKzt = 4_500L,
                status = CashbackStatus.CONFIRMED,
                createdAtEpochMs = COFFEE_CREATED_EPOCH_MS,
                confirmedAtEpochMs = COFFEE_CONFIRMED_EPOCH_MS,
            ),
            entry(
                id = "seed-pending-tech",
                offer = SampleCatalog.requireOffer("nomad-tech"),
                purchaseKzt = 12_000L,
                status = CashbackStatus.PENDING,
                createdAtEpochMs = TECH_CREATED_EPOCH_MS,
                confirmedAtEpochMs = null,
            ),
        ),
    )

    init {
        val seeded = state()
        require(seeded.confirmedKzt == 225L)
        require(seeded.pendingKzt == 840L)
    }

    private fun entry(
        id: String,
        offer: Offer,
        purchaseKzt: Long,
        status: CashbackStatus,
        createdAtEpochMs: Long,
        confirmedAtEpochMs: Long?,
    ): CashbackEntry = CashbackEntry(
        id = id,
        offerId = offer.id,
        merchantName = offer.merchantName,
        samplePurchaseKzt = purchaseKzt,
        sampleRatePercent = offer.sampleRatePercent,
        cashbackKzt = CashbackRules.cashbackKzt(purchaseKzt, offer.sampleRatePercent),
        status = status,
        createdAtEpochMs = createdAtEpochMs,
        confirmedAtEpochMs = confirmedAtEpochMs,
        note = DemoCopy.ENTRY_NOTE,
    )
}
