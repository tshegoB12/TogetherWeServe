package com.togetherweserve.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

/**
 * Builds the Retrofit client for the Cloud Firestore REST API (free Spark plan).
 * The Firebase ID token is attached to every call so firestore.rules apply.
 */
object RetrofitClient {

    private const val PROJECT_ID = "togetherweserve-39d3f"
    private const val BASE_URL =
        "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/"

    private val authInterceptor = Interceptor { chain ->
        val token = runBlocking {
            try {
                FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
            } catch (e: Exception) {
                null
            }
        }
        val request = chain.request().newBuilder().apply {
            if (token != null) addHeader("Authorization", "Bearer $token")
        }.build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val rest: FirestoreRest by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FirestoreRest::class.java)
    }

    /** Free-tier "REST API": the Firestore REST API wrapped behind the app's ApiService. */
    val firestoreApi: FirestoreApi by lazy { FirestoreApi(rest, PROJECT_ID) }
    val api: ApiService get() = firestoreApi
}