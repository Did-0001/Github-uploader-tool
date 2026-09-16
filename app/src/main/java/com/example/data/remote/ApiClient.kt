package com.example.data.remote

import com.example.data.local.AuthPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AuthInterceptor(private val authPreferences: AuthPreferences) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val original = chain.request()
    val builder = original.newBuilder()
      .header("Accept", "application/vnd.github.v3+json")
      .header("User-Agent", "GitUploader-Android-App")

    val token = authPreferences.getToken()
    if (!token.isNullOrBlank()) {
      builder.header("Authorization", "Bearer $token")
    }

    return chain.proceed(builder.build())
  }
}

object ApiClient {
  val moshi: Moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  fun createGitHubService(authPreferences: AuthPreferences): GitHubApiService {
    val logging = HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BASIC
    }

    val okHttpClient = OkHttpClient.Builder()
      .connectTimeout(60, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .writeTimeout(60, TimeUnit.SECONDS)
      .addInterceptor(AuthInterceptor(authPreferences))
      .addInterceptor(logging)
      .build()

    return Retrofit.Builder()
      .baseUrl("https://api.github.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(GitHubApiService::class.java)
  }

  fun createOAuthService(): GitHubOAuthService {
    val okHttpClient = OkHttpClient.Builder()
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .build()

    return Retrofit.Builder()
      .baseUrl("https://github.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(GitHubOAuthService::class.java)
  }
}
