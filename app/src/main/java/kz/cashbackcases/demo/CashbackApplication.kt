package kz.cashbackcases.demo

import android.app.Application
import kz.cashbackcases.core.AppRepository
import kz.cashbackcases.core.FileLedgerStorage
import kz.cashbackcases.demo.wallet.DeviceIdStore
import kz.cashbackcases.demo.wallet.WalletApiClient
import java.io.File

class CashbackApplication : Application() {
    lateinit var repository: AppRepository
        private set
    lateinit var walletApi: WalletApiClient
        private set
    lateinit var deviceIdStore: DeviceIdStore
        private set

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(FileLedgerStorage(File(filesDir, "demo-ledger.json")))
        walletApi = WalletApiClient(BuildConfig.API_BASE_URL.trim().trimEnd('/'))
        deviceIdStore = DeviceIdStore(this)
    }
}
