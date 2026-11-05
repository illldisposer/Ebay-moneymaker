package com.example.data.api

import android.util.Base64
import com.example.data.local.entity.BotSettingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class EbayItem(
    val itemId: String,
    val title: String,
    val price: Double,
    val shipping: Double,
    val currency: String,
    val imageUrl: String,
    val itemUrl: String,
    val condition: String
)

class EbayApiException(message: String) : Exception(message)

/**
 * Offizielle eBay Browse API mit Application Token (Client-Credentials-Grant).
 * Kein Passwort-Login, kein Scraping.
 */
class EbayBrowseClient(private val http: OkHttpClient) {

    private var cachedToken: String? = null
    private var cachedTokenKey: String = ""
    private var tokenExpiresAt: Long = 0L

    private fun baseUrl(settings: BotSettingEntity): String =
        if (settings.ebayUseSandbox) "https://api.sandbox.ebay.com" else "https://api.ebay.com"

    private fun fetchToken(settings: BotSettingEntity): String {
        if (!settings.hasEbayKeys()) throw EbayApiException("eBay Client ID / Client Secret fehlen (Einstellungen).")

        val key = "${settings.ebayUseSandbox}|${settings.ebayClientId}"
        val now = System.currentTimeMillis()
        cachedToken?.let { token ->
            if (key == cachedTokenKey && now < tokenExpiresAt - 60_000L) return token
        }

        val credentials = "${settings.ebayClientId.trim()}:${settings.ebayClientSecret.trim()}"
        val basic = Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        val body = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("scope", "https://api.ebay.com/oauth/api_scope")
            .build()

        val request = Request.Builder()
            .url("${baseUrl(settings)}/identity/v1/oauth2/token")
            .header("Authorization", "Basic $basic")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(body)
            .build()

        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw EbayApiException("eBay-Token fehlgeschlagen (HTTP ${response.code}): ${extractError(text)}")
            }
            val json = JSONObject(text)
            val token = json.optString("access_token")
            if (token.isBlank()) throw EbayApiException("eBay-Token-Antwort ohne access_token.")
            val expiresIn = json.optLong("expires_in", 7200L)
            cachedToken = token
            cachedTokenKey = key
            tokenExpiresAt = now + expiresIn * 1000L
            return token
        }
    }

    /**
     * Sucht neue Sofort-Kaufen-Angebote mit Lieferung nach Deutschland.
     * Entweder per Suchtext [query] oder exakt per [gtin] (EAN/UPC).
     */
    suspend fun search(
        settings: BotSettingEntity,
        query: String? = null,
        gtin: String? = null,
        limit: Int = 5
    ): List<EbayItem> = withContext(Dispatchers.IO) {
        val token = fetchToken(settings)

        val urlBuilder = "${baseUrl(settings)}/buy/browse/v1/item_summary/search".toHttpUrl().newBuilder()
        if (!query.isNullOrBlank()) urlBuilder.addQueryParameter("q", query)
        if (!gtin.isNullOrBlank()) urlBuilder.addQueryParameter("gtin", gtin)
        urlBuilder.addQueryParameter("limit", limit.coerceIn(1, 50).toString())
        urlBuilder.addQueryParameter("filter", "buyingOptions:{FIXED_PRICE},conditionIds:{1000},deliveryCountry:DE")

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("Authorization", "Bearer $token")
            .header("X-EBAY-C-MARKETPLACE-ID", settings.ebayMarketplaceId.ifBlank { "EBAY_DE" })
            .header("X-EBAY-C-ENDUSERCTX", "contextualLocation=country%3DDE")
            .header("Accept-Language", "de-DE")
            .get()
            .build()

        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (response.code == 401) {
                cachedToken = null
                throw EbayApiException("eBay lehnt den Token ab (401). Client ID/Secret und Sandbox-Schalter prüfen.")
            }
            if (!response.isSuccessful) {
                throw EbayApiException("eBay-Suche fehlgeschlagen (HTTP ${response.code}): ${extractError(text)}")
            }
            parseItems(JSONObject(text))
        }
    }

    /**
     * Geschätzte Verkaufsmenge eines Angebots aus der Einzelabfrage (estimatedAvailabilities).
     * Liefert null, wenn eBay keine Angabe macht. Klick- oder Aufrufzahlen liefert eBay nicht.
     */
    suspend fun fetchSoldEstimate(settings: BotSettingEntity, itemId: String): Int? = withContext(Dispatchers.IO) {
        val token = fetchToken(settings)
        val url = "${baseUrl(settings)}/buy/browse/v1/item".toHttpUrl().newBuilder()
            .addPathSegment(itemId)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("X-EBAY-C-MARKETPLACE-ID", settings.ebayMarketplaceId.ifBlank { "EBAY_DE" })
            .header("X-EBAY-C-ENDUSERCTX", "contextualLocation=country%3DDE")
            .header("Accept-Language", "de-DE")
            .get()
            .build()

        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) return@use null
            val arr = JSONObject(text).optJSONArray("estimatedAvailabilities") ?: return@use null
            var best: Int? = null
            for (i in 0 until arr.length()) {
                val entry = arr.optJSONObject(i) ?: continue
                if (!entry.has("estimatedSoldQuantity")) continue
                val sold = entry.optInt("estimatedSoldQuantity", -1)
                if (sold >= 0) best = maxOf(best ?: 0, sold)
            }
            best
        }
    }

    suspend fun testConnection(settings: BotSettingEntity): String = withContext(Dispatchers.IO) {
        val items = search(settings, query = "usb kabel", limit = 1)
        val env = if (settings.ebayUseSandbox) "Sandbox" else "Produktion"
        "eBay ($env) OK – Token erhalten, Testsuche lieferte ${items.size} Treffer."
    }

    private fun parseItems(json: JSONObject): List<EbayItem> {
        val arr = json.optJSONArray("itemSummaries") ?: return emptyList()
        val result = mutableListOf<EbayItem>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val priceObj = o.optJSONObject("price")
            val price = priceObj?.optString("value")?.toDoubleOrNull() ?: continue
            val currency = priceObj.optString("currency", "EUR")

            var shipping = 0.0
            val shipArr = o.optJSONArray("shippingOptions")
            if (shipArr != null && shipArr.length() > 0) {
                shipping = shipArr.optJSONObject(0)
                    ?.optJSONObject("shippingCost")
                    ?.optString("value")
                    ?.toDoubleOrNull() ?: 0.0
            }

            val image = o.optJSONObject("image")?.optString("imageUrl").orEmpty().ifBlank {
                o.optJSONArray("thumbnailImages")?.optJSONObject(0)?.optString("imageUrl").orEmpty()
            }

            result.add(
                EbayItem(
                    itemId = o.optString("itemId"),
                    title = o.optString("title"),
                    price = price,
                    shipping = shipping,
                    currency = currency,
                    imageUrl = image,
                    itemUrl = o.optString("itemWebUrl"),
                    condition = o.optString("condition")
                )
            )
        }
        return result.filter { it.itemId.isNotBlank() && it.title.isNotBlank() }
    }

    private fun extractError(body: String): String {
        return try {
            val json = JSONObject(body)
            val errors = json.optJSONArray("errors")
            when {
                errors != null && errors.length() > 0 -> {
                    val e = errors.optJSONObject(0)
                    e?.optString("longMessage").orEmpty().ifBlank { e?.optString("message").orEmpty() }
                }
                json.has("error_description") -> json.optString("error_description")
                json.has("error") -> json.optString("error")
                else -> body.take(200)
            }
        } catch (e: Exception) {
            body.take(200)
        }
    }
}
