package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getStreamerReaction(
        streamerName: String,
        gameName: String,
        userMessage: String,
        userDonationAmount: Long = 0L,
        userDonationMessage: String = ""
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "API Key is template placeholder.")
            return@withContext getDefaultReaction(streamerName, userMessage, userDonationAmount, userDonationMessage)
        }

        val prompt = if (userDonationAmount > 0) {
            "Seseorang mendonasikan Rp $userDonationAmount dengan pesan: '$userDonationMessage'. Berikan reaksi heboh dan ucapkan terima kasih banyak!"
        } else {
            "Ada pesan chat: '$userMessage'. Berikan tanggapan singkat yang seru."
        }

        val systemInstruction = """
            Kamu adalah $streamerName, seorang streamer game terkenal dari Indonesia yang sedang live streaming game: '$gameName'.
             
            Aturan:
            1. Balas pesan/donasi penonton dengan gaya khas kamu (misalnya jika Windah Basudara: heboh, sebut penonton 'teman-teman', tulus, dramatis, suka teriak gembira. Jika Lemon MLBB: agak dingin/cuek tapi jago, kalem, panggil 'guys'. Jika MiawAug: ramah banget, sopan, panggil 'halo semuanya kembali lagi bersama saya MiawAug').
            2. Gunakan bahasa Indonesia kasual, gaul, ekspresif, interaktif seolah berbicara langsung di live stream.
            3. Jawab dengan SANGAT SINGKAT (maksimal 1-2 kalimat pendek). Jangan bertele-tele!
            4. Balas seolah-olah kamu sedang membaca live chat saat bermain game.
        """.trimIndent()

        try {
            // Build json body manually with JSONObject to be extremely robust
            val jsonBody = JSONObject().apply {
                val contentsArray = org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                }
                put("contents", contentsArray)

                val systemInstructionObj = JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                }
                put("systemInstruction", systemInstructionObj)

                val configObj = JSONObject().apply {
                    put("temperature", 0.8)
                    put("maxOutputTokens", 120)
                }
                put("generationConfig", configObj)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string()
                    Log.e(TAG, "Error Response: $errBody")
                    return@withContext getDefaultReaction(streamerName, userMessage, userDonationAmount, userDonationMessage)
                }
                val bodyStr = response.body?.string() ?: ""
                val jsonObj = JSONObject(bodyStr)
                val candidates = jsonObj.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val text = parts.getJSONObject(0).getString("text")
                text?.trim() ?: getDefaultReaction(streamerName, userMessage, userDonationAmount, userDonationMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during call: ${e.message}", e)
            getDefaultReaction(streamerName, userMessage, userDonationAmount, userDonationMessage)
        }
    }

    private fun getDefaultReaction(
        streamer: String,
        msg: String,
        donationAmount: Long,
        donationMsg: String
    ): String {
        return if (donationAmount > 0) {
            when (streamer) {
                "Windah Basudara" -> "KEMURAHAN REJEKI?! Gila sih! Terima kasih banyak Rp $donationAmount ya teman-teman, semoga lancar rezekinya selalu! Wah, mantap jaya!"
                "Lemon MLBB" -> "Wih, makasih ya donasinya Rp $donationAmount. Lumayan buat beli kopi weh, thank you ya."
                "MiawAug" -> "Halo! Wah, terima kasih banyak kak untuk donasinya sebesar Rp $donationAmount. Semoga sehat selalu dan dilancarkan rezekinya ya!"
                "Luthfi Halimawan" -> "Gila gila gila! Rp $donationAmount meluncur! Thank you beneran brader, mantap banget dah supportnya!"
                else -> "Wih, terima kasih banyak ya donasinya kak! Semoga dilancarkan terus rezekinya!"
            }
        } else {
            when (streamer) {
                "Windah Basudara" -> {
                    val responses = listOf(
                        "Wkwk halo juga teman-teman! Masuk akal sih itu, kita gas terus game ini!",
                        "Jangan lupa subscribe ya teman-teman! Kita mau tamatin game ini malam ini!",
                        "Bentar-bentar guys, ini musuhnya kok susah banget ya wkwkwk."
                    )
                    responses.random()
                }
                "Lemon MLBB" -> {
                    val responses = listOf(
                        "Halo ya. Ini tinggal kita push aja lord-nya, gampang ini guys.",
                        "Bisa mabar nanti, kelarin ini dulu ya.",
                        "Marksman gak boleh dimanja sih, biarin aja."
                    )
                    responses.random()
                }
                "MiawAug" -> {
                    val responses = listOf(
                        "Halo semuanya, selamat datang ya! Kali ini kita lanjutin lagi gamenya, seru banget ini!",
                        "Waduh waduh, itu apa tadi lewat?! Serem banget guys wkwkwk.",
                        "Makasih bapak, makasih semuanya yang udah mampir nonton ya!"
                    )
                    responses.random()
                }
                else -> "Halo juga! Selamat datang di live streaming kita ya! Seru banget nih turnamennya."
            }
        }
    }
}
