package kz.cashbackcases.demo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kz.cashbackcases.core.ActivationResult
import kz.cashbackcases.core.AppRepository
import kz.cashbackcases.core.ConfirmResult
import kz.cashbackcases.core.LedgerState
import kz.cashbackcases.core.TopupSnapshot
import kz.cashbackcases.core.TopupStatus
import kz.cashbackcases.core.WalletCopy
import kz.cashbackcases.core.WalletRules
import kz.cashbackcases.core.WalletSnapshot
import kz.cashbackcases.demo.wallet.DeviceIdStore
import kz.cashbackcases.demo.wallet.WalletApiClient

data class WalletUiState(
    val apiConfigured: Boolean,
    val loading: Boolean = false,
    val wallet: WalletSnapshot? = null,
    val activeTopup: TopupSnapshot? = null,
    val showAmountPicker: Boolean = false,
)

class CashbackViewModel(
    private val repository: AppRepository,
    private val walletApi: WalletApiClient,
    private val deviceIdStore: DeviceIdStore,
) : ViewModel() {
    val state: StateFlow<LedgerState> = repository.state

    private val _walletUi = MutableStateFlow(
        WalletUiState(apiConfigured = walletApi.configured),
    )
    val walletUi: StateFlow<WalletUiState> = _walletUi.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var pollJob: Job? = null

    init {
        refreshWallet()
    }

    fun refreshWallet() {
        if (!walletApi.configured) {
            _walletUi.value = WalletUiState(apiConfigured = false)
            return
        }
        viewModelScope.launch {
            _walletUi.value = _walletUi.value.copy(loading = true, apiConfigured = true)
            val deviceId = deviceIdStore.getOrCreate()
            val result = walletApi.getWallet(deviceId)
            result.onSuccess { wallet ->
                _walletUi.value = _walletUi.value.copy(loading = false, wallet = wallet)
            }.onFailure { error ->
                _walletUi.value = _walletUi.value.copy(loading = false)
                _messages.tryEmit(error.message ?: "Не удалось загрузить кошелёк.")
            }
        }
    }

    fun openTopupPicker() {
        if (!walletApi.configured) {
            _messages.tryEmit(WalletCopy.NOT_CONFIGURED)
            return
        }
        _walletUi.value = _walletUi.value.copy(showAmountPicker = true)
    }

    fun dismissTopupPicker() {
        _walletUi.value = _walletUi.value.copy(showAmountPicker = false)
    }

    fun startTopup(amountKzt: Long) {
        val error = WalletRules.topupError(amountKzt)
        if (error != null) {
            _messages.tryEmit(error)
            return
        }
        viewModelScope.launch {
            _walletUi.value = _walletUi.value.copy(showAmountPicker = false, loading = true)
            val deviceId = deviceIdStore.getOrCreate()
            val result = walletApi.createTopup(deviceId, amountKzt)
            result.onSuccess { topup ->
                _walletUi.value = _walletUi.value.copy(loading = false, activeTopup = topup)
                startPolling(topup.topupId)
            }.onFailure { err ->
                _walletUi.value = _walletUi.value.copy(loading = false)
                _messages.tryEmit(err.message ?: "Не удалось начать пополнение.")
            }
        }
    }

    fun dismissActiveTopup() {
        pollJob?.cancel()
        pollJob = null
        _walletUi.value = _walletUi.value.copy(activeTopup = null)
        refreshWallet()
    }

    private fun startPolling(topupId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(2_500)
                val result = walletApi.refreshTopup(topupId)
                result.onSuccess { topup ->
                    _walletUi.value = _walletUi.value.copy(
                        activeTopup = topup,
                        wallet = _walletUi.value.wallet?.copy(balanceKzt = topup.walletBalanceKzt),
                    )
                    when (topup.status) {
                        TopupStatus.PAID -> {
                            _messages.tryEmit(WalletCopy.PAID)
                            return@launch
                        }
                        TopupStatus.EXPIRED, TopupStatus.CANCELLED, TopupStatus.ERROR -> {
                            _messages.tryEmit(topup.errorMessage ?: WalletCopy.FAILED)
                            return@launch
                        }
                        else -> Unit
                    }
                }.onFailure { err ->
                    _messages.tryEmit(err.message ?: "Ошибка опроса статуса.")
                    return@launch
                }
            }
        }
    }

    fun activate(offerId: String, purchaseKzt: Long) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.activate(offerId, purchaseKzt) }
            val message = when (result) {
                is ActivationResult.Activated -> null
                ActivationResult.AlreadyPending -> "Для этого предложения уже есть запись в статусе «ожидает»."
                is ActivationResult.UnknownOffer -> "Предложение не найдено."
                is ActivationResult.InvalidPurchase -> result.reason
            }
            if (message != null) _messages.tryEmit(message)
        }
    }

    fun confirmLocally(entryId: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.confirmLocally(entryId) }
            val message = when (result) {
                is ConfirmResult.Confirmed -> null
                ConfirmResult.NotPending -> "Запись уже не в статусе «ожидает»."
                ConfirmResult.UnknownEntry -> "Запись не найдена."
            }
            if (message != null) _messages.tryEmit(message)
        }
    }

    fun reset() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.reset() }
            _messages.tryEmit("Демо-данные сброшены.")
        }
    }

    companion object {
        fun factory(
            repository: AppRepository,
            walletApi: WalletApiClient,
            deviceIdStore: DeviceIdStore,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CashbackViewModel(repository, walletApi, deviceIdStore) as T
                }
            }
        }
    }
}
