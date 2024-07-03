package com.example.umbrellacatcher.weatherapi

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherInterface {
    @GET("/1360000/VilageFcstInfoService_2.0/getVilageFcst")
    fun getWeather(
        @Query("serviceKey") key: String,
        @Query("numOfRows") rows: Int,
        @Query("pageNo") pages: Int,
        @Query("dataType") type: String,

        @Query("base_date") date: String,
        @Query("base_time") time: String,
        @Query("nx") nx: Int,
        @Query("ny") ny: Int
    ): Call<WeatherData>
}