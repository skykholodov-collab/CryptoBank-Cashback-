package kz.cashbackcases.demo.ui

object Tags {
    const val DEMO_BANNER = "demo-banner"
    const val OPEN_COLLECTION = "open-collection"
    const val ACTIVATE = "activate-submit"
    const val CONFIRM_ACTIVATE = "dialog-confirm-activate"
    const val ACTIVATED = "activated-panel"
    const val NAV_CASES = "nav-cases"
    const val NAV_BALANCE = "nav-balance"
    const val BALANCE_PENDING = "balance-pending"
    const val BALANCE_CONFIRMED = "balance-confirmed"
    const val WITHDRAWAL = "withdrawal-button"
    const val RESET = "reset-demo"
    const val CONFIRM_RESET = "dialog-confirm-reset"
    const val CONFIRM_STATUS = "dialog-confirm-status"

    fun caseTab(id: String): String = "case-tab-$id"

    fun caseCard(id: String): String = "case-$id"

    fun offer(id: String): String = "offer-$id"

    fun purchase(amount: Long): String = "purchase-$amount"

    fun confirmOffer(id: String): String = "confirm-offer-$id"
}
