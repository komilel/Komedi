package com.komi.komedi.data.remote

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OpenFdaResponse(
    val results: List<DrugResult>? = null,
    val error: OpenFdaError? = null
)

@Serializable
data class OpenFdaError(
    val code: String? = null,
    val message: String? = null
)

@Serializable
data class DrugResult(
    @SerialName("openfda")
    val openFda: OpenFdaInfo? = null,
    @SerialName("purpose")
    val purpose: List<String>? = null,
    @SerialName("warnings")
    val warnings: List<String>? = null,
    @SerialName("dosage_and_administration")
    val dosageAndAdministration: List<String>? = null,
    @SerialName("indications_and_usage")
    val indicationsAndUsage: List<String>? = null
)

@Serializable
data class OpenFdaInfo(
    @SerialName("brand_name")
    val brandName: List<String>? = null,
    @SerialName("generic_name")
    val genericName: List<String>? = null,
    @SerialName("manufacturer_name")
    val manufacturerName: List<String>? = null,
    @SerialName("route")
    val route: List<String>? = null
)

data class DrugInfo(
    val brandName: String,
    val genericName: String?,
    val manufacturer: String?,
    val purpose: String?,
    val warnings: String?,
    val dosageInfo: String?,
    val usage: String?
)

class DrugApiService {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun searchDrug(name: String): Result<List<DrugInfo>> {
        return try {
            // Clean and prepare the search term
            val cleanName = name.trim().lowercase()

            // Use wildcard search for better matching
            // Search in both brand_name and generic_name
            val searchQuery = "(openfda.brand_name:$cleanName*+OR+openfda.generic_name:$cleanName*)"

            val url = "https://api.fda.gov/drug/label.json?search=$searchQuery&limit=10"

            Log.d("DrugApiService", "Searching: $url")

            val responseText: String = client.get(url).bodyAsText()

            Log.d("DrugApiService", "Response: ${responseText.take(500)}")

            val response = json.decodeFromString<OpenFdaResponse>(responseText)

            if (response.error != null) {
                Log.e("DrugApiService", "API Error: ${response.error.message}")
                return Result.success(emptyList())
            }

            val drugs = response.results?.mapNotNull { result ->
                val brandName = result.openFda?.brandName?.firstOrNull()
                val genericName = result.openFda?.genericName?.firstOrNull()

                // Need at least a brand name or generic name
                val displayName = brandName ?: genericName ?: return@mapNotNull null

                DrugInfo(
                    brandName = displayName,
                    genericName = if (brandName != null) genericName else null,
                    manufacturer = result.openFda?.manufacturerName?.firstOrNull(),
                    purpose = result.purpose?.firstOrNull()?.take(200),
                    warnings = result.warnings?.firstOrNull()?.take(300),
                    dosageInfo = result.dosageAndAdministration?.firstOrNull()?.take(300),
                    usage = result.indicationsAndUsage?.firstOrNull()?.take(200)
                )
            }?.distinctBy { it.brandName.lowercase() }?.take(5) ?: emptyList()

            Log.d("DrugApiService", "Found ${drugs.size} drugs")

            Result.success(drugs)
        } catch (e: Exception) {
            Log.e("DrugApiService", "Error searching drugs", e)
            Result.failure(e)
        }
    }

    fun close() {
        client.close()
    }
}
