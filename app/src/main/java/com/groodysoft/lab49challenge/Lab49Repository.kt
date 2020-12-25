package com.groodysoft.lab49challenge

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET


data class Lab49Item(val id: Int, val name: String)

interface Lab49API {

    @GET("iositems/items")
    suspend fun getItems(): List<Lab49Item>

}

object Lab49Repository {

    lateinit var currentItemsToSnap: List<Lab49Item>

    suspend fun getItems() = lab49API.getItems()
}

val lab49API:Lab49API by lazy {

    val interceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val client = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .build()

    Retrofit.Builder()
        .baseUrl("https://hoi4nusv56.execute-api.us-east-1.amazonaws.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(MainApplication.gson))
        .build().create(Lab49API::class.java)
}





