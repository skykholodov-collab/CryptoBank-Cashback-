package kz.cashbackcases.demo.wallet

import android.content.Context
import java.util.UUID

class DeviceIdStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getOrCreate(): String {
        val existing = prefs.getString(KEY, null)
        if (!existing.isNullOrBlank()) return existing
        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY, created).apply()
        return created
    }

    companion object {
        private const val PREFS = "wallet_device"
        private const val KEY = "device_id"
    }
}
