package com.example.data.api

import com.example.data.local.entity.BotSettingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class AliProduct(
    val productId: String,
    val title: String,
    val price: Double,
    val originalPrice: Double,
    val currency: String,
    val imageUrl: String,
    val productUrl: String,
    val affiliateUrl: String,
    val salesVolume: Int,
    val commissionRate: String,
    val category: String
)

class AliExpressApiException(message: String) : Exception(message)

/**
 * Offizielle AliExpress Affiliate API (Open Platform, Gateway /sync, Signatur HMAC-SHA256).
 */
class AliExpressClient(private val http: OkHttpClient) {

    private val gateway = "https://api-sg.aliexpress.com/sync"

    /** Produktsuche nach Stichwort, sortiert nach Verkaufszahlen. Preise in EUR, Versand nach DE. */
    suspend fun searchProducts(
        settings: BotSettingEntity,
        keyword: String,
        pageSize: Int
    ): List<AliProduct> = withContext(Dispatchers.IO) {
        if (!settings.hasAliKeys()) throw AliExpressApiException("AliExpress App Key / App Secret / Tracking-ID fehlen (Einstellungen).")

        val method = "aliexpress.affiliate.product.query"
        val params = sortedMapOf(
            "app_key" to settings.aliAppKey.trim(),
            "method" to method,
            "sign_method" to "sha256",
            "timestamp" to System.currentTimeMillis().toString(),
            "keywords" to keyword,
            "page_no" to "1",
            "page_size" to pageSize.coerceIn(1, 50).toString(),
            "sort" to "LAST_VOLUME_DESC",
            "target_currency" to "EUR",
            "target_language" to "DE",
            "ship_to_country" to "DE",
            "tracking_id" to settings.aliTrackingId.trim()
        )
        params["sign"] = sign(params, settings.aliAppSecret.trim())

        val form = FormBody.Builder()
        params.forEach { (k, v) -> form.add(k, v) }

        val request = Request.Builder()
            .url(gateway)
            .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
            .post(form.build())
            .build()

        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AliExpressApiException("AliExpress HTTP ${response.code}: ${text.take(200)}")
            }
            parseProducts(JSONObject(text), "aliexpress_affiliate_product_query_response")
        }
    }

    suspend fun testConnection(settings: BotSettingEntity): String = withContext(Dispatchers.IO) {
        val products = searchProducts(settings, "phone holder", 1)
        "AliExpress OK – Signatur akzeptiert, Testsuche lieferte ${products.size} Produkt(e)."
    }

    private fun sign(params: Map<String, String>, secret: String): String {
        // Signatur-String: alle Parameter (ohne "sign") alphabetisch, Schlüssel+Wert direkt aneinander
        val sb = StringBuilder()
        params.toSortedMap().forEach { (k, v) -> if (k != "sign") sb.append(k).append(v) }
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val bytes = mac.doFinal(sb.toString().toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02X".format(it) }
    }

    private fun parseProducts(root: JSONObject, responseKey: String): List<AliProduct> {
        // Fehlerformate der Open Platform
        root.optJSONObject("error_response")?.let { err ->
            throw AliExpressApiException("AliExpress-Fehler ${err.optString("code")}: ${err.optString("msg")} ${err.optString("sub_msg")}".trim())
        }
        if (root.has("code") && root.optString("code") != "0" && !root.has(responseKey)) {
            throw AliExpressApiException("AliExpress-Fehler ${root.optString("code")}: ${root.optString("message")}")
        }

        val resp = root.optJSONObject(responseKey)
            ?: throw AliExpressApiException("Unerwartete AliExpress-Antwort: ${root.toString().take(200)}")
        val respResult = resp.optJSONObject("resp_result")
            ?: throw AliExpressApiException("AliExpress-Antwort ohne resp_result.")
        val code = respResult.optInt("resp_code", -1)
        if (code != 200) {
            // 405 = keine Treffer für das Stichwort
            if (code == 405) return emptyList()
            throw AliExpressApiException("AliExpress resp_code $code: ${respResult.optString("resp_msg")}")
        }

        val arr = respResult.optJSONObject("result")
            ?.optJSONObject("products")
            ?.optJSONArray("product") ?: return emptyList()

        val result = mutableListOf<AliProduct>()
        for (i in 0 until arr.length()) {
            val p = arr.optJSONObject(i) ?: continue
            val price = p.optString("target_sale_price").toDoubleOrNull()
                ?: p.optString("sale_price").toDoubleOrNull()
                ?: continue
            result.add(
                AliProduct(
                    productId = p.optString("product_id"),
                    title = p.optString("product_title"),
                    price = price,
                    originalPrice = p.optString("target_original_price").toDoubleOrNull() ?: price,
                    currency = p.optString("target_sale_price_currency", "EUR"),
                    imageUrl = p.optString("product_main_image_url"),
                    productUrl = p.optString("product_detail_url"),
                    affiliateUrl = p.optString("promotion_link"),
                    salesVolume = p.optInt("lastest_volume", 0),
                    commissionRate = p.optString("commission_rate"),
                    category = p.optString("second_level_category_name").ifBlank { p.optString("first_level_category_name") }
                )
            )
        }
        return result.filter { it.productId.isNotBlank() && it.title.isNotBlank() }
    }
}
