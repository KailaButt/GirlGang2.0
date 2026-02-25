package com.example.consolicalm

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class ZenQuoteDto(
    val q: String, // quote text
    val a: String  // author
)

interface ZenQuotesApi {
    // Daily quote: https://zenquotes.io/api/today :contentReference[oaicite:1]{index=1}
    @GET("api/today")
    suspend fun getTodayQuote(): List<ZenQuoteDto>

    // Optional alternative: random quote: https://zenquotes.io/api/random :contentReference[oaicite:2]{index=2}
    @GET("api/random")
    suspend fun getRandomQuote(): List<ZenQuoteDto>
}

object ZenQuotesClient {
    val api: ZenQuotesApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://zenquotes.io/") // must end with /
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ZenQuotesApi::class.java)
    }
}