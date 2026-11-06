package kz.cashbackcases.demo

import android.app.Application
import kz.cashbackcases.core.AppRepository
import kz.cashbackcases.core.FileLedgerStorage
import java.io.File

class CashbackApplication : Application() {
    lateinit var repository: AppRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(FileLedgerStorage(File(filesDir, "demo-ledger.json")))
    }
}
