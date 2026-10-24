package com.craftcaller.app.data.remote

import com.craftcaller.app.BuildConfig
import com.craftcaller.app.data.model.LookupApiResponse
import com.craftcaller.app.data.model.ReportSpamRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Own REST lookup API (Retrofit). Optional: Firebase is the primary caller-ID
 * source; this is the fallback endpoint used when Firebase is unavailable.
 */
interface CraftCallerApi {

    @GET("lookup")
    suspend fun lookup(
        @Query("number") number: String,
        @Header("Authorization") apiKey: String = "Bearer ${BuildConfig.API_KEY}"
    ): LookupApiResponse?

    @POST("reports")
    suspend fun reportSpam(
        @Body request: ReportSpamRequest,
        @Header("Authorization") apiKey: String = "Bearer ${BuildConfig.API_KEY}"
    ): LookupApiResponse?
}

/** Single Retrofit client. */
object RetrofitClient {
    val api: CraftCallerApi by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(CraftCallerApi::class.java)
    }
}
