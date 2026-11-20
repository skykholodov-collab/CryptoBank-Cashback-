package kz.cashbackcases.core

import kotlinx.serialization.Serializable

object WalletRules {
    const val MIN_TOPUP_KZT = 1_000L
    const val MAX_TOPUP_KZT = 100_000L
    val presetsKzt: List<Long> = listOf(1_000L, 5_000L, 10_000L)

    fun topupError(amountKzt: Long): String? {
        if (amountKzt < MIN_TOPUP_KZT || amountKzt > MAX_TOPUP_KZT) {
            return "Сумма пополнения должна быть от ${formatKzt(MIN_TOPUP_KZT)} до ${formatKzt(MAX_TOPUP_KZT)}."
        }
        return null
    }
}

@Serializable
enum class TopupStatus {
    CREATED,
    PENDING,
    PAID,
    EXPIRED,
    CANCELLED,
    ERROR,
}

@Serializable
data class WalletSnapshot(
    val deviceId: String,
    val balanceKzt: Long,
    val sandbox: Boolean,
    val configured: Boolean,
    val recent: List<WalletLedgerItem> = emptyList(),
)

@Serializable
data class WalletLedgerItem(
    val id: String,
    val amountKzt: Long,
    val kind: String,
    val note: String,
    val createdAt: String,
)

@Serializable
data class TopupSnapshot(
    val topupId: String,
    val apipayInvoiceId: Long? = null,
    val amountKzt: Long,
    val status: TopupStatus,
    val qrImageUrl: String? = null,
    val qrTokenUrl: String? = null,
    val expiresAt: String? = null,
    val errorMessage: String? = null,
    val walletBalanceKzt: Long,
    val sandbox: Boolean,
)

object WalletCopy {
    const val SECTION_TITLE = "Кошелёк"
    const val SECTION_INTRO =
        "Отдельный баланс пополнений. Он не смешивается с образцовым кешбэком и не обещает доход."
    const val TOPUP_BUTTON = "Пополнить через Kaspi Pay"
    const val TOPUP_TITLE = "Пополнение кошелька"
    const val TOPUP_HINT = "Оплата через ApiPay поверх Kaspi Pay. Ключи хранятся только на сервере."
    const val QR_TITLE = "Отсканируйте QR в приложении Kaspi"
    const val QR_SANDBOX = "Тестовый режим: реальная оплата не выполняется."
    const val NOT_CONFIGURED =
        "Пополнение недоступно: нет адреса сервера. Укажите API_BASE_URL в local.properties."
    const val WAITING = "Ожидаем оплату…"
    const val PAID = "Кошелёк пополнен."
    const val FAILED = "Пополнение не завершено."
}
