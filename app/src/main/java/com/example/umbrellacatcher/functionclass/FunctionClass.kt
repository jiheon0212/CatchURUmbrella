package com.example.umbrellacatcher.functionclass

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FunctionClass {
    // 현재 시간을 기상청 api 업데이트 주기에 맞춰주는 method
    fun getCurrentUpdate(currentTime: String): String {
        val timeSwitch =
            when (currentTime) {
                "0200", "0300", "0400" -> "0200"
                "0500", "0600", "0700" -> "0500"
                "0800", "0900", "1000" -> "0800"
                "1100", "1200", "1300" -> "1100"
                "1400", "1500", "1600" -> "1400"
                "1700", "1800", "1900" -> "1700"
                "2000", "2100", "2200" -> "2000"

                else -> "2300"
            }

        //Log.d("tester", timeSwitch)
        return timeSwitch
    }

    // 현재 시간을 가져오는 method
    fun getCurrentTime(): String {
        val currentTime = SimpleDateFormat("HH00", Locale.KOREA)
        currentTime.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        val resultTime = currentTime.format(Date().time)

        //Log.d("tester", resultTime)
        return resultTime
    }

    // 현재 날짜를 가져오는 method
    fun getCurrentDate(): String {
        val currentDate = SimpleDateFormat("yyyyMMdd", Locale.KOREA)
        val resultDate = currentDate.format(Date())

        //Log.d("tester", resultDate)
        return resultDate
    }


}