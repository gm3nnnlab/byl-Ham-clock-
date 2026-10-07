package com.hamdash.chornobyl.network

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

/** Fetches any absolute https URL; callers parse the body themselves so no source is hard-wired. */
interface RawApi {
    @GET
    suspend fun fetch(@Url url: String): ResponseBody
}

object NetworkFactory {
    const val USER_AGENT = "ChornobylHamDash/0.2 (amateur radio reference app)"

    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
        }
        .build()

    fun rawApi(client: OkHttpClient): RawApi =
        Retrofit.Builder()
            .baseUrl("https://localhost/")
            .client(client)
            .build()
            .create(RawApi::class.java)
}

object PropagationSources {
    const val SOLAR_FLUX = "https://services.swpc.noaa.gov/products/summary/10cm-flux.json"
    const val K_INDEX = "https://services.swpc.noaa.gov/products/noaa-planetary-k-index.json"
    const val FLARES = "https://services.swpc.noaa.gov/json/goes/primary/xray-flares-latest.json"
    const val ATTRIBUTION = "NOAA Space Weather Prediction Center (services.swpc.noaa.gov)"
}
