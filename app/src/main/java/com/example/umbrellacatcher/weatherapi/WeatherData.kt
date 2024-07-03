package com.example.umbrellacatcher.weatherapi

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class WeatherData(
    @Expose
    @SerializedName("response")
    val response: Response
) {
    data class Response(
        @Expose
        @SerializedName("header")
        val header: Header,
        @Expose
        @SerializedName("body")
        val body: Body
    ) {
        data class Body(
            @Expose
            @SerializedName("dataType")
            var dataType: String,
            @Expose
            @SerializedName("items")
            val items: Items,
            @Expose
            @SerializedName("numOfRows")
            var rows: Int,
            @Expose
            @SerializedName("pageNo")
            var pages: Int,
            @Expose
            @SerializedName("totalCount")
            var totalCount: Int,
        ) {
            data class Items(
                @Expose
                @SerializedName("item")
                val item: ArrayList<Item>
            ) {
                data class Item(
                    @Expose
                    @SerializedName("base_date")
                    var date: String,
                    @Expose
                    @SerializedName("base_time")
                    var time: String,
                    @Expose
                    @SerializedName("category")
                    var category: String,
                    @Expose
                    @SerializedName("fcstDate")
                    var fcstDate: String,
                    @Expose
                    @SerializedName("fcstTime")
                    var fcstTime: String,
                    @Expose
                    @SerializedName("fcstValue")
                    var fcstValue: String,
                    @Expose
                    @SerializedName("nx")
                    var nx: Int,
                    @Expose
                    @SerializedName("ny")
                    var ny: Int,
                )
            }
        }
        data class Header(
            @Expose
            @SerializedName("resultCode")
            val resultCode: String,
            @Expose
            @SerializedName("resultMsg")
            val resultMsg: String
        )
    }
}