package kz.cashbackcases.demo.wallet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kz.cashbackcases.core.TopupSnapshot
import kz.cashbackcases.core.WalletSnapshot
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class WalletApiClient(
    private val baseUrl: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
) {
    val configured: Boolean
        get() = baseUrl.isNotBlank()

    suspend fun getWallet(deviceId: String): Result<WalletSnapshot> = withContext(Dispatchers.IO) {
        if (!configured) return@withContext Result.failure(IllegalStateException("API_BASE_URL is empty"))
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/v1/wallet?deviceId=${encode(deviceId)}")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error(parseMessage(body) ?: "Ошибка кошелька (${response.code})")
                }
                json.decodeFromString(WalletSnapshot.serializer(), body)
            }
        }
    }

    suspend fun createTopup(deviceId: String, amountKzt: Long): Result<TopupSnapshot> =
        withContext(Dispatchers.IO) {
            if (!configured) return@withContext Result.failure(IllegalStateException("API_BASE_URL is empty"))
            runCatching {
                val payload = json.encodeToString(
                    CreateTopupRequest.serializer(),
                    CreateTopupRequest(deviceId = deviceId, amountKzt = amountKzt),
                )
                val request = Request.Builder()
                    .url("$baseUrl/v1/wallet/topups")
                    .post(payload.toRequestBody(JSON))
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        error(parseMessage(body) ?: "Не удалось создать пополнение (${response.code})")
                    }
                    json.decodeFromString(TopupSnapshot.serializer(), body)
                }
            }
        }

    suspend fun refreshTopup(topupId: String): Result<TopupSnapshot> = withContext(Dispatchers.IO) {
        if (!configured) return@withContext Result.failure(IllegalStateException("API_BASE_URL is empty"))
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/v1/wallet/topups/${encode(topupId)}")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error(parseMessage(body) ?: "Не удалось обновить статус (${response.code})")
                }
                json.decodeFromString(TopupSnapshot.serializer(), body)
            }
        }
    }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun parseMessage(body: String): String? {
        if (body.isBlank()) return null
        return runCatching {
            json.decodeFromString(ErrorBody.serializer(), body).message
        }.getOrNull()
    }

    @Serializable
    private data class CreateTopupRequest(
        val deviceId: String,
        val amountKzt: Long,
    )

    @Serializable
    private data class ErrorBody(val message: String? = null)

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
