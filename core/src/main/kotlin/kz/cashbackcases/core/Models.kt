package kz.cashbackcases.core

import kotlinx.serialization.Serializable

enum class CaseAccent {
    CYAN,
    GOLD,
    SPLIT,
}

data class Offer(
    val id: String,
    val collectionId: String,
    val merchantName: String,
    val category: String,
    val sampleRatePercent: Int,
    val blurb: String,
    val terms: List<String>,
)

data class OfferCollection(
    val id: String,
    val title: String,
    val subtitle: String,
    val accent: CaseAccent,
    val offers: List<Offer>,
) {
    val maxSampleRatePercent: Int get() = offers.maxOf { it.sampleRatePercent }
}

@Serializable
enum class CashbackStatus {
    PENDING,
    CONFIRMED,
}

@Serializable
data class CashbackEntry(
    val id: String,
    val offerId: String,
    val merchantName: String,
    val samplePurchaseKzt: Long,
    val sampleRatePercent: Int,
    val cashbackKzt: Long,
    val status: CashbackStatus,
    val createdAtEpochMs: Long,
    val confirmedAtEpochMs: Long?,
    val note: String,
)

data class LedgerState(
    val entries: List<CashbackEntry>,
) {
    val pendingKzt: Long
        get() = entries.filter { it.status == CashbackStatus.PENDING }.sumOf { it.cashbackKzt }

    val confirmedKzt: Long
        get() = entries.filter { it.status == CashbackStatus.CONFIRMED }.sumOf { it.cashbackKzt }
}
