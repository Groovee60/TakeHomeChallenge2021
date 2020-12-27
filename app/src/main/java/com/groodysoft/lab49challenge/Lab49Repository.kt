package com.groodysoft.lab49challenge

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


data class Lab49ServerItem(val id: Int, val name: String)

data class ImagePayload(val name: String, val base64ImageBytes: String)

data class ImagePostResponse(val matched: Boolean)

interface Lab49API {

    @GET("iositems/items")
    suspend fun getItems(): List<Lab49ServerItem>

    @POST("iositems/items")
    suspend fun postItem(@Body body: ImagePayload): ImagePostResponse
}

object Lab49Repository {

    suspend fun getItems() = lab49API.getItems()

    suspend fun postItem(payload: ImagePayload) = lab49API.postItem(payload)
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





