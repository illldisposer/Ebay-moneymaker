package com.example.data.gemini

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ProductComparisonResult(
    val sameProduct: Boolean,
    val confidence: Int, // 0-100: Wahrscheinlichkeit, dass es exakt derselbe Artikel ist
    val reason: String
)

class GeminiApiException(message: String) : Exception(message)

/**
 * Gemini-Bild- und Titelvergleich: Ist das AliExpress-Produkt derselbe Artikel wie das eBay-Angebot?
 */
class GeminiResonanceService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun resolveApiKey(settingsKey: String): String? {
        val key = settingsKey.trim().ifBlank { BuildConfig.GEMINI_API_KEY.trim() }
        return if (key.isBlank() || key == "MY_GEMINI_API_KEY") null else key
    }

    /**
     * Gibt null zurück, wenn kein Gemini-Schlüssel vorhanden ist.
     * Wirft [GeminiApiException] bei API-Fehlern (falsches Modell, Kontingent, ...).
     */
    suspend fun compareProducts(
        apiKey: String?,
        model: String,
        aliTitle: String,
        aliImageUrl: String,
        ebayTitle: String,
        ebayImageUrl: String
    ): ProductComparisonResult? = withContext(Dispatchers.IO) {
        if (apiKey == null) return@withContext null

        val prompt = """
            Du prüfst für einen Preisvergleich, ob zwei Online-Angebote EXAKT derselbe Artikel sind
            (gleiches Modell, gleiche Variante, gleiche Menge/Größe) – nicht nur dieselbe Produktart.
            Bild 1 / Titel A = AliExpress, Bild 2 / Titel B = eBay.

            Titel A: $aliTitle
            Titel B: $ebayTitle

            Achte auf Form, Farbe, Aufdrucke, Anschlüsse, Lieferumfang, Stückzahl und Maße.
            "confidence" = Wahrscheinlichkeit in Prozent (0-100), dass es exakt derselbe Artikel ist.
            Antworte nur als JSON:
            {"sameProduct": true, "confidence": 0, "reason": "kurze Begründung auf Deutsch, max. 20 Wörter"}
        """.trimIndent()

        val parts = JSONArray().put(JSONObject().put("text", prompt))
        downloadImage(aliImageUrl)?.let { (mime, data) ->
            parts.put(JSONObject().put("inline_data", JSONObject().put("mime_type", mime).put("data", data)))
        }
        downloadImage(ebayImageUrl)?.let { (mime, data) ->
            parts.put(JSONObject().put("inline_data", JSONObject().put("mime_type", mime).put("data", data)))
        }

        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseMimeType", "application/json")
                    .put("temperature", 0.1)
            )
            .toString()

        val modelName = model.trim().ifBlank { "gemini-flash-latest" }
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent")
            .header("x-goog-api-key", apiKey)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val maxAttempts = 4
        for (attempt in 1..maxAttempts) {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.isSuccessful) return@withContext parseResult(text)
                if (response.code != 429 || attempt == maxAttempts) {
                    val msg = try {
                        JSONObject(text).optJSONObject("error")?.optString("message").orEmpty()
                    } catch (e: Exception) {
                        ""
                    }
                    throw GeminiApiException("Gemini HTTP ${response.code} ($modelName): ${msg.ifBlank { text.take(200) }}")
                }
                Log.w("GeminiService", "Rate-Limit, warte vor Versuch ${attempt + 1}")
            }
            delay(15_000L * attempt)
        }
        throw GeminiApiException("Gemini Rate-Limit ($modelName) – später erneut scannen.")
    }

    private fun parseResult(responseBody: String): ProductComparisonResult {
        val json = JSONObject(responseBody)
        val text = json.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")
            .orEmpty()
        if (text.isBlank()) throw GeminiApiException("Gemini lieferte keine Antwort (evtl. Sicherheitsfilter).")

        val cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val parsed = JSONObject(cleaned)
        val same = parsed.optBoolean("sameProduct", false)
        var confidence = parsed.optInt("confidence", 0).coerceIn(0, 100)
        // "Nicht derselbe Artikel" darf nie als sicher durchrutschen
        if (!same) confidence = minOf(confidence, 35)
        return ProductComparisonResult(
            sameProduct = same,
            confidence = confidence,
            reason = parsed.optString("reason").ifBlank { if (same) "Gleicher Artikel laut KI" else "Abweichender Artikel laut KI" }
        )
    }

    /** Lädt ein Produktbild (max. 4 MB) und gibt (MIME-Typ, Base64) zurück. */
    private fun downloadImage(url: String): Pair<String, String>? {
        if (url.isBlank()) return null
        return try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bytes = response.body?.bytes() ?: return null
                if (bytes.isEmpty() || bytes.size > 4 * 1024 * 1024) return null
                val headerType = response.header("Content-Type")?.substringBefore(";")?.trim().orEmpty()
                val mime = when {
                    headerType.startsWith("image/") -> headerType
                    url.contains(".png", ignoreCase = true) -> "image/png"
                    url.contains(".webp", ignoreCase = true) -> "image/webp"
                    else -> "image/jpeg"
                }
                mime to Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            Log.w("GeminiService", "Bild nicht ladbar: $url (${e.message})")
            null
        }
    }
}
