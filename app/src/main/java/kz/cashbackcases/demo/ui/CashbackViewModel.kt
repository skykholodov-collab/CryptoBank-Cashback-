package kz.cashbackcases.demo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kz.cashbackcases.core.ActivationResult
import kz.cashbackcases.core.AppRepository
import kz.cashbackcases.core.ConfirmResult
import kz.cashbackcases.core.LedgerState

class CashbackViewModel(
    private val repository: AppRepository,
) : ViewModel() {
    val state: StateFlow<LedgerState> = repository.state

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

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
        fun factory(repository: AppRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CashbackViewModel(repository) as T
                }
            }
        }
    }
}
