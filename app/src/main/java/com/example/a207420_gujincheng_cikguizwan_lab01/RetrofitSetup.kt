package com.example.a207420_gujincheng_cikguizwan_lab01

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class MyMemoryResponse(
    val responseStatus: Int,
    val responseData: ResponseData
)

data class ResponseData(
    val translatedText: String
)

interface MyMemoryApi {
    @GET("get")
    suspend fun translate(
        @Query("q") text: String,
        @Query("langpair") langPair: String = "en|zh"
    ): MyMemoryResponse
}

object RetrofitClient {
    val api: MyMemoryApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.mymemory.translated.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MyMemoryApi::class.java)
    }
}
