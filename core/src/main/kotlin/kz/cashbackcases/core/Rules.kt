package kz.cashbackcases.core

object CashbackRules {
    const val MAX_SAMPLE_RATE_PERCENT = 10
    const val MIN_SAMPLE_PURCHASE_KZT = 1_000L
    const val MAX_SAMPLE_PURCHASE_KZT = 100_000L

    fun cashbackKzt(purchaseKzt: Long, ratePercent: Int): Long {
        require(ratePercent in 1..MAX_SAMPLE_RATE_PERCENT) {
            "Sample rate must be from 1 to $MAX_SAMPLE_RATE_PERCENT percent."
        }
        require(purchaseKzt >= 0L)
        return purchaseKzt * ratePercent / 100L
    }

    fun purchaseError(purchaseKzt: Long): String? {
        if (purchaseKzt < MIN_SAMPLE_PURCHASE_KZT || purchaseKzt > MAX_SAMPLE_PURCHASE_KZT) {
            return "Сумма покупки должна быть от ${formatKzt(MIN_SAMPLE_PURCHASE_KZT)} до ${formatKzt(MAX_SAMPLE_PURCHASE_KZT)}."
        }
        return null
    }
}

object SamplePurchases {
    val presetsKzt: List<Long> = listOf(4_500L, 12_000L, 27_500L)

    init {
        presetsKzt.forEach { amount ->
            require(CashbackRules.purchaseError(amount) == null) { amount }
        }
    }
}

fun formatKzt(amount: Long): String {
    val negative = amount < 0
    val digits = kotlin.math.abs(amount).toString()
    val grouped = digits.reversed().chunked(3).joinToString(" ").reversed()
    val body = if (negative) "−$grouped" else grouped
    return "$body ₸"
}

object DemoCopy {
    const val ENTRY_NOTE = "Вымышленный образец. Деньги не переводились."
    const val PURCHASE_HINT = "Это пример суммы покупки, а не взнос и не пополнение баланса."
    const val CALC_DISCLAIMER =
        "Целые тенге, дробная часть отбрасывается. Цифра не обещает выплату и не фиксирует доход."
    const val CONFIRM_LOCAL_BODY =
        "Статус сменится на «подтверждено» только в локальной истории этого устройства. Выплата не выполняется, реквизиты не запрашиваются."
    const val RESET_BODY = "История вернётся к начальному вымышленному образцу. Это не операция с деньгами."

    fun activationDialog(purchaseKzt: Long, cashbackKzt: Long, ratePercent: Int): String {
        return "Активировать демо-отслеживание для вымышленной покупки на ${formatKzt(purchaseKzt)}? " +
            "Расчётный образец: ${formatKzt(cashbackKzt)} ($ratePercent%). " +
            "Деньги не списываются и не отправляются."
    }

    fun sampleTerms(ratePercent: Int): List<String> = listOf(
        "Ставка $ratePercent% — вымышленный образец. Она не обещает доход.",
        "В рабочей модели начисление возможно только после квалифицирующей покупки и только из комиссии продавца.",
        "Прототип не принимает оплату, не продаёт открытие подборки и не платит за счёт новых участников.",
        "Продавец, покупка и сумма вымышлены. Ссылки на реальные магазины нет.",
        "Смена статуса хранится только на устройстве. Вывод в демо недоступен.",
    )
}

object WithdrawalPolicy {
    const val availableInDemo: Boolean = false
    const val message: String =
        "Вывод недоступен в этой демонстрации. Приложение не подключает платёжного провайдера, не собирает реквизиты и не отправляет деньги."

    fun request(state: LedgerState): WithdrawalDecision {
        check(!availableInDemo) { "Demo must not enable a withdrawal." }
        return WithdrawalDecision.Unavailable(message = message, unchanged = state)
    }
}

sealed interface WithdrawalDecision {
    val message: String
    val unchanged: LedgerState

    data class Unavailable(
        override val message: String,
        override val unchanged: LedgerState,
    ) : WithdrawalDecision
}
