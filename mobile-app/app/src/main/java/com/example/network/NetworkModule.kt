package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    // OkHttp's defaults (10s) are too short for a Render free-tier backend, which
    // spins down when idle and can take 30-60s to cold-start on the first request.
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val baseUrl: String =
        if (BuildConfig.API_BASE_URL.endsWith("/")) BuildConfig.API_BASE_URL
        else "${BuildConfig.API_BASE_URL}/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val moshiInstance: Moshi = moshi

    val potholeApiService: PotholeApiService = retrofit.create(PotholeApiService::class.java)
    val uploadApiService: UploadApiService = retrofit.create(UploadApiService::class.java)
    val userApiService: UserApiService = retrofit.create(UserApiService::class.java)
}
