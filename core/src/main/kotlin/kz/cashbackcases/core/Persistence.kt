package kz.cashbackcases.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

interface LedgerStorage {
    fun read(): LedgerState?
    fun write(state: LedgerState)
}

@Serializable
data class PersistedLedger(
    val entries: List<CashbackEntry>,
)

class FileLedgerStorage(
    private val file: File,
) : LedgerStorage {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    private val lock = Any()

    override fun read(): LedgerState? = synchronized(lock) {
        if (!file.exists() || file.length() == 0L) return null
        return try {
            LedgerState(json.decodeFromString(PersistedLedger.serializer(), file.readText()).entries)
        } catch (_: Exception) {
            null
        }
    }

    override fun write(state: LedgerState) {
        synchronized(lock) {
            val dir = file.absoluteFile.parentFile ?: File(".")
            dir.mkdirs()
            val target = File(dir, file.name)
            val tmp = File(dir, "${file.name}.tmp")
            tmp.writeText(json.encodeToString(PersistedLedger(state.entries)))
            if (!tmp.renameTo(target)) {
                target.writeText(tmp.readText())
                tmp.delete()
            }
        }
    }
}

class AppRepository(
    private val storage: LedgerStorage,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val ids: () -> String = { UUID.randomUUID().toString() },
) {
    private val lock = Any()
    private val ledger: DemoLedger
    private val _state: MutableStateFlow<LedgerState>
    val state: StateFlow<LedgerState>

    init {
        val loaded = storage.read()
        val initial = loaded ?: DemoSeed.state()
        if (loaded == null) storage.write(initial)
        ledger = DemoLedger(initial = initial, clock = clock, ids = ids)
        _state = MutableStateFlow(ledger.snapshot())
        state = _state.asStateFlow()
    }

    fun activate(offerId: String, purchaseKzt: Long): ActivationResult = synchronized(lock) {
        val result = ledger.activate(offerId, purchaseKzt)
        persist()
        result
    }

    fun confirmLocally(entryId: String): ConfirmResult = synchronized(lock) {
        val result = ledger.confirmLocally(entryId)
        persist()
        result
    }

    fun reset() {
        synchronized(lock) {
            ledger.replace(DemoSeed.state())
            persist()
        }
    }

    private fun persist() {
        val snap = ledger.snapshot()
        storage.write(snap)
        _state.value = snap
    }
}
