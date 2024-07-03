package com.example.umbrellacatcher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.umbrellacatcher.databinding.ActivityLocationBinding
import com.example.umbrellacatcher.functionclass.FunctionClass
import com.example.umbrellacatcher.weatherapi.WeatherData
import com.example.umbrellacatcher.weatherapi.WeatherInfo
import com.example.umbrellacatcher.weatherapi.WeatherService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import jxl.Workbook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.io.InputStream
import java.util.Locale

class LocationActivity : AppCompatActivity() {
    lateinit var activityLocationBinding: ActivityLocationBinding
    lateinit var addressList: List<Address>
    val functionClass = FunctionClass()
    val mainScope = MainScope()
    val weatherDataList = mutableListOf<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityLocationBinding = ActivityLocationBinding.inflate(layoutInflater)
        val view = activityLocationBinding.root
        setContentView(view)
    }

    override fun onResume() {
        super.onResume()

        // todo - 작동 순서: 권한 검사 -> 지역 -> 엑셀 시트 값 -> 날씨 데이터 -> 메인 복귀
        // todo - 메인 복귀 데이터 반환 값: 지역명, sky, pty, pcp, tmp, pop
        // todo - locationActivity 시작시, progressBar가 로딩되고 현재 주소값을 받아온 후에 dialog에 띄워 사용자의 취소/확인을 전달받는다.
        // todo - dialog를 화면에 띄워 주소가 맞는지 물어보고 틀렸을 경우에 method 다시 호출 맞았을 경우에 textview에 표시
        // todo - textview에 주소가 등록되면 현재 주소 사용하기 btn 활성화

        mainScope.launch {
            activityLocationBinding.progressBar.visibility = View.VISIBLE
            withContext(Dispatchers.IO) {
                getCurrentLocation()
                delay(4500)
            }
            activityLocationBinding.progressBar.visibility = View.GONE
            cancel()
        }

        // todo - addTextChangeListener 사용으로 textview에 주소값이 들어오거나 수정되면 readExcel method 작동시키기
        // todo - method 진행 방향 getWeatherData(readExcel("죽전1동").first, readExcel("죽전1동").second)
        // todo - btn click시, 현재 주소와 날씨 데이터를 apiResultData로 mainActivity에 넘겨준다.

        activityLocationBinding.tvLocationNameHolder.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val address = activityLocationBinding.tvLocationNameHolder.text
                val splitAddress = address.split(" ")
                // Log.d("tester", splitAddress.get(4))

                val getNxNy = readExcel(splitAddress[4])
                // Log.d("tester", "${getNxNy.first}, ${getNxNy.second}")
                getWeatherData(getNxNy.first, getNxNy.second)
            }
        })
    }

    // locationActivity에서 필요한 데이터 값을 모두 충족하면 mainActivity로 돌아가는 method
    private fun callMainActivity(weatherDataResultList: MutableList<Pair<String, String>>) {
        val returnMainIntent = Intent()
        val weatherDataResult = mutableMapOf<String, String>()

        for (weatherData in weatherDataResultList) {
            weatherDataResult += mapOf(weatherData.first to weatherData.second)
        }
        weatherDataResult += mapOf("address" to "${activityLocationBinding.tvLocationNameHolder.text}")
        // weatherDataResult 값은 정상으로 작동함

        // 오류 해결 완료: getWeatherData method에서 if문 밖에 callMainActivity를 호출해야 작동한다.

        val apiResultData = ApiResultData(
            address = weatherDataResult["address"] ?: "",
            tmp = weatherDataResult["TMP"] ?: "",
            sky = weatherDataResult["SKY"] ?: "",
            pty = weatherDataResult["PTY"] ?: "",
            pop = weatherDataResult["POP"] ?: "",
            pcp = weatherDataResult["PCP"] ?: ""
        )

        returnMainIntent.putExtra("location", apiResultData)
        setResult(RESULT_OK, returnMainIntent)

        finish()
    }

    // 기상청 api를 통해 json 데이터 형식의 파일에서 값을 추출하는 method
    private fun getWeatherData(nx: Int, ny: Int) {
        val weather = WeatherService.weatherInterface.getWeather(
            WeatherInfo.API_KEY,
            10,
            1,
            "json",
            functionClass.getCurrentDate(),
            functionClass.getCurrentUpdate(functionClass.getCurrentTime())
            ,nx
            ,ny
        )

        weather.enqueue(object: retrofit2.Callback<WeatherData> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(p0: Call<WeatherData>, p1: Response<WeatherData>) {
                val resultCode = p1.body()?.response?.header?.resultCode
                val resultMsg = p1.body()?.response?.header?.resultMsg
                val excludedWeatherData = listOf("UUU", "VVV", "VEC", "WSD", "WAV")

                if (resultCode == "00") {
                    p1.body()?.response?.body?.items?.item?.forEach {
                        // "pcp, 0" , "sky, 1", "pop, 0" -> pair 객체로 담아와서 분리
                        if (it.category !in excludedWeatherData) {
                            // Log.d("tester", "${it.category}, ${it.fcstValue}")
                            weatherDataList.add(Pair(it.category, it.fcstValue))
                        }
                    }
                    // weatherDataList 변경 완료 후 적용할 method
                    // Log.d("tester", "$weatherDataList")
                    callMainActivity(weatherDataList)
                } else {
                    Toast.makeText(this@LocationActivity, "Error: $resultMsg", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(p0: Call<WeatherData>, p1: Throwable) {
                Toast.makeText(this@LocationActivity, "Error: $p1", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 현재 lat, lng 값을 가져오는 method
    private fun getCurrentLocation() {
        // gms service dependency에서 가져옴
        val locationManager = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // here to request the missing permissions, and then overriding
            return
        }
        locationManager.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener {
                val address = getAddressFromLocation(it.latitude, it.longitude)?.get(0)
                // 현재 주소 값이 들어있는 상수
                val resultAddress = address?.getAddressLine(0)
                activityLocationBinding.tvLocationNameHolder.text = resultAddress
            }
    }

    // 현재 lat, lng에 기반하여 geoCoder를 사용해 주소를 불러오는 method
    private fun getAddressFromLocation(latitude: Double, longitude: Double): List<Address>? {
        return try {
            val geocoder = Geocoder(this, Locale.KOREA)
            addressList = geocoder.getFromLocation(latitude, longitude, 1) as List<Address>
            addressList
        } catch (e: IOException) {
            null
        }
    }

    // 엑셀 파일 읽는 method
    private fun readExcel(localName: String?): Pair<Int, Int> {
        val localFile: InputStream = baseContext.resources.assets.open("local_name_sheets.xls")
        val workbook: Workbook = Workbook.getWorkbook(localFile)
        var nnx: String? = null
        var nny: String? = null

        workbook.getSheet(0).let {
            val columTotal = it.columns
            val rowIndexStart = 1
            val rowTotal = it.getColumn(columTotal - 1).size
            var row = rowIndexStart
            while (row < rowTotal) {
                val contents = it.getCell(4, row).contents
                if (contents.contains(localName!!)) {
                    nnx = it.getCell(5, row).contents
                    nny = it.getCell(6, row).contents
                    row = rowTotal
                }
                row++
            }
        }

        return Pair(nnx!!.toInt(), nny!!.toInt())
    }
}