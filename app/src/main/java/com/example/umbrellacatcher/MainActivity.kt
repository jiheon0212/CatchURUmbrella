package com.example.umbrellacatcher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.umbrellacatcher.databinding.ActivityMainBinding
import com.example.umbrellacatcher.functionclass.FunctionClass
import com.example.umbrellacatcher.weatherapi.WeatherData
import com.example.umbrellacatcher.weatherapi.WeatherInfo
import com.example.umbrellacatcher.weatherapi.WeatherService
import retrofit2.Call
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    lateinit var activityMainBinding: ActivityMainBinding
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
    val functionClass = FunctionClass()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        val view = activityMainBinding.root
        setContentView(view)
        checkAndRequestPermissions()
        getWeatherData()

        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val data: Intent? = it.data
                // todo - SDK version 확인해서 if문으로 분기해주기
                val locationResult: ApiResultData? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    data?.getSerializableExtra("location", ApiResultData::class.java)
                    } else {
                    @Suppress("DEPRECATION")
                    data?.getSerializableExtra("location") as? ApiResultData
                    }

                locationResult.let {
                    // todo - apiResultData에서 받아온 값들로 view에 binding해주기
                    // apiResultData값 정상적으로 받아와 작동되는 것 확인
                    // Log.d("tester", "$it")
                    activityMainBinding.tvLocationName.text = it?.address
                    activityMainBinding.tvTmp.text = "${it?.tmp}℃"
                    it?.sky.let {
                        when (it) {
                            "1" -> activityMainBinding.tvSky.text = "현재 날씨는 맑은 상태로"
                            "3" -> activityMainBinding.tvSky.text = "현재 날씨는 구름 많은 상태로"
                            "4" -> activityMainBinding.tvSky.text = "현재 날씨는 흐린 상태로"
                        }
                    }
                    activityMainBinding.tvRainPopPcp.text = "강수 확률은 ${it?.pop}%이며 강수량은 ${it?.pcp} 입니다."
                    it?.pty.let {
                        when (it) {
                            "0" -> activityMainBinding.tvPty.text = "아무것도 내리지 않습니다."
                            "1" -> activityMainBinding.tvPty.text = "비가 내립니다."
                            "2" -> activityMainBinding.tvPty.text = "눈이 내립니다."
                            "3" -> activityMainBinding.tvPty.text = "비/눈이 내립니다."
                            "4" -> activityMainBinding.tvPty.text = "소나기가 내립니다."
                        }
                    }

                    changeWeatherImage(it?.sky, it?.pty)
                }
            }
        }

        activityMainBinding.btnChangeLocation.setOnClickListener {
            val intent = Intent(this, LocationActivity::class.java)
            resultLauncher.launch(intent)
        }
    }

    // 권한 검사
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Toast.makeText(this, "All permissions are already granted", Toast.LENGTH_SHORT).show()
        }
    }

    // 날씨 코드와 강수 형태 코드를 받아와 img_weather를 교체해주는 method
    private fun changeWeatherImage(weatherCode: String?, weatherType: String?) {
        when (weatherCode) {
            "1" -> {
                Glide.with(this).load(R.drawable.sun_foreground).into(activityMainBinding.imgWeather)
            }
            "3" -> {
                Glide.with(this).load(R.drawable.cloud_foreground).into(activityMainBinding.imgWeather)
                /*when (weatherType) {
                    "0" -> { Glide.with(this).load("uri holder").into(activityMainBinding.imgWeather) }
                    "1" -> { Glide.with(this).load("uri holder").into(activityMainBinding.imgWeather) }
                    "2" -> { Glide.with(this).load("uri holder").into(activityMainBinding.imgWeather) }
                    "3" -> { Glide.with(this).load("uri holder").into(activityMainBinding.imgWeather) }
                    "4" -> { Glide.with(this).load("uri holder").into(activityMainBinding.imgWeather) }
                }*/
            }
            "4" -> {
                Glide.with(this).load(R.drawable.cloud_foreground).into(activityMainBinding.imgWeather)
                /*when (weatherType) {
                    "0" -> { Glide.with(this).load("uri holder").into(activityMainBinding.imgWeather) }
                    "1" -> { Glide.with(this).load("uri holder").into(activityMainBinding.imgWeather) }
                    "2" -> { Glide.with(this).load("uri holder").into(activityMainBinding.imgWeather) }
                    "3" -> { Glide.with(this).load("uri holder").into(activityMainBinding.imgWeather) }
                    "4" -> { Glide.with(this).load("uri holder").into(activityMainBinding.imgWeather) }
                }*/
            }
        }
    }

    // 기상청 api를 통해 json 데이터 형식의 파일에서 값을 추출하는 method
    private fun getWeatherData() {
        val weather = WeatherService.weatherInterface.getWeather(
            WeatherInfo.API_KEY,
            10,
            1,
            "json",
            functionClass.getCurrentDate(),
            functionClass.getCurrentUpdate(functionClass.getCurrentTime())
            ,60
            ,127
        )

        weather.enqueue(object: retrofit2.Callback<WeatherData> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(p0: Call<WeatherData>, p1: Response<WeatherData>) {
                val resultCode = p1.body()?.response?.header?.resultCode
                val resultMsg = p1.body()?.response?.header?.resultMsg
                var sky = ""
                var pty = ""
                var pcp = ""
                var tmp = ""
                var pop = ""

                if (resultCode == "00") {
                    p1.body()?.response?.body?.items?.item?.forEach {
                        when (it.category) {
                            // 값 모두 받아와서 data class 객체로 값 받아오기
                            "SKY" -> { sky = it.fcstValue } // 하늘상태
                            "PTY" -> { pty = it.fcstValue } // 강수형태
                            "PCP" -> { pcp = it.fcstValue } // 강수량
                            "TMP" -> { tmp = it.fcstValue } // 현재기온
                            "POP" -> { pop = it.fcstValue } // 강수확률
                        }
                    }
                    activityMainBinding.tvTmp.text = "$tmp℃"
                    sky.let {
                        when (it) {
                            "1" -> activityMainBinding.tvSky.text = "현재 날씨는 맑은 상태로"
                            "3" -> activityMainBinding.tvSky.text = "현재 날씨는 구름 많은 상태로"
                            "4" -> activityMainBinding.tvSky.text = "현재 날씨는 흐린 상태로"
                        }
                    }
                    activityMainBinding.tvRainPopPcp.text = "강수 확률은 ${pop}%이며 강수 형태는 ${pcp} 입니다."
                    pty.let {
                        when (it) {
                            "0" -> activityMainBinding.tvPty.text = "아무것도 내리지 않습니다."
                            "1" -> activityMainBinding.tvPty.text = "비가 내립니다."
                            "2" -> activityMainBinding.tvPty.text = "눈이 내립니다."
                            "3" -> activityMainBinding.tvPty.text = "비/눈이 내립니다."
                            "4" -> activityMainBinding.tvPty.text = "소나기가 내립니다."
                        }
                    }
                    activityMainBinding.tvLocationName.text = getString(R.string.location_first_setting)

                } else {
                    Toast.makeText(this@MainActivity, "Error: $resultMsg", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(p0: Call<WeatherData>, p1: Throwable) {
                Toast.makeText(this@MainActivity, "Error: $p1", Toast.LENGTH_SHORT).show()
            }
        })
    }
}