package com.example.consolicalm

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

/**
 * Daily Challenge source.
 * Uses the App Brewery Bored API fork (no API key required).
 */
data class DailyChallengeDto(
    val activity: String?
)

interface DailyChallengeApi {
    @GET("random")
    suspend fun getRandomChallenge(): DailyChallengeDto
}

object DailyChallengeClient {
    val api: DailyChallengeApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://bored-api.appbrewery.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DailyChallengeApi::class.java)
    }
}
